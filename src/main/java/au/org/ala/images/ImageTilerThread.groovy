package au.org.ala.images

import au.org.ala.images.tiling.ImageTiler
import au.org.ala.images.tiling.ImageTilerConfig
import au.org.ala.images.tiling.ImageTilerResults
import au.org.ala.images.tiling.TileFormat
import au.org.ala.images.util.CodeTimer
import groovy.json.JsonSlurper
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.InputStreamBody
import org.apache.http.entity.mime.content.StringBody

import java.awt.Color

class ImageTilerThread extends Thread {

    private File _workDir
    private String _serviceBase = "http://images.ala.org.au"
    private int _waitingPeriodSeconds = 10

    public ImageTilerThread(File workDirectory, String serviceBase, int waitingPeriod) {

        this.setDaemon(false)
        this.name = "Image Tiling Agent Main"
        this._serviceBase = serviceBase
        this._waitingPeriodSeconds = waitingPeriod
        _workDir = workDirectory
    }

    @Override
    void run() {

        _workDir.mkdirs()

        while (true) {

            def url = new URL("${_serviceBase}/ws/getNextTileJob");
            def text = url.text

            def parser = new JsonSlurper()
            def job = parser.parseText(text)

            if (job.success) {
                // do job...
                try {
                    processJob(job)
                } catch (Exception ex) {
                    // try and cancel the job
                    ex.printStackTrace()
                    Logger.log("Attempting to cancel job...")
                    def cancelUrl = new URL("${_serviceBase}/ws/cancelTileJob?ticket=${job.jobTicket}")
                    //Logger.log(cancelUrl.text)
                    // re-throw
                    throw ex
                }
            } else {
                Logger.log("No job: ${job.message} Waiting...")
                Thread.sleep(_waitingPeriodSeconds * 1000)
            }

        }
    }

    def processJob(Object job) {
        Logger.log("Job recieved: Ticket ${job.jobTicket} for image ${job.imageId}")
        def ticket = job.jobTicket
        def imageId = job.imageId
        def imageInfo = getImageInfo(imageId)
        if (imageInfo) {
            Logger.log("Image size (w x h): ${imageInfo.width} x ${imageInfo.height} Size: ${imageInfo.sizeInBytes} type: ${imageInfo.mimeType}")
            def imageFile = downloadImage(imageInfo)
            if (imageFile) {
                try {
                    def results = tileImage(imageFile, TileFormat.JPEG)
                    if (results.zoomLevels > 0) {
                        // zip the tms directory
                        def archiveFile = createTileArchive(imageId)
                        if (archiveFile && archiveFile.exists()) {
                            try {
                                // send the archive back to the server...
                                if (postTileArchive(archiveFile, ticket, results)) {
                                    Logger.log("Tiling complete")
                                }
                            } finally {
                                Logger.log("Deleting archive file...")
                                archiveFile.delete()
                            }
                        }
                    }
                } finally {
                    Logger.log("Deleting image file...")
                    imageFile.delete()
                }
            }
        }

    }

    private boolean postTileArchive(File file, String ticket, ImageTilerResults tileResults) {
        Logger.log("Sending tiles to server...")
        CodeTimer ct = new CodeTimer("Posting archive")
        def params = [jobTicket: ticket, zoomLevels: tileResults.zoomLevels]
        def results = postMultipart("${_serviceBase}/ws/postJobResults", params, file, "application/zip")
        Logger.logTimer(ct)
        return results?.status == 200
    }

    private createTileArchive(String imageId) {
        Logger.log("Creating zip file...")
        def ct = new CodeTimer("Archiving tiles")
        def ant = new AntBuilder();
        def archivePath = _workDir.absolutePath + "/${imageId}_tiles.zip"
        ant.zip(
            basedir: _workDir.absolutePath + "/tms/",
            destfile: archivePath,
            compress: true
        )
        Logger.logTimer(ct)
        return new File(archivePath)
    }

    private ImageTilerResults tileImage(File file, TileFormat tileFormat) {

        def dest = new File(_workDir.absolutePath + "/tms/")
        dest.delete()

        Logger.log("Tiling image...")
        def ct = new CodeTimer("Tiling image")
        try {
            def config = new ImageTilerConfig(2, 2, 256, 6, tileFormat)
            config.tileBackgroundColor = new Color(221, 221, 221)
            def tiler = new ImageTiler(config)
            return tiler.tileImage(file, dest)
        } finally {
            Logger.logTimer(ct)
        }
    }

    private Object getImageInfo(String imageId) {
        def url = new URL("${_serviceBase}/ws/getImageInfo/${imageId}");
        def parser = new JsonSlurper()
        def imageInfo = parser.parseText(url.text)
        return imageInfo
    }

    private File downloadImage(Object imageInfo) {
        Logger.log("Getting image file from ${imageInfo.imageUrl}")
        def ct = new CodeTimer("Downloading file ${imageInfo.imageUrl}")
        def url = new URL(imageInfo.imageUrl)
        def file = new File(_workDir.absolutePath + "/" + imageInfo.originalFileName)

        file.parentFile.mkdirs()

        file.createNewFile()
        file.newOutputStream() << url.newInputStream()
        if (file.exists() && file.length() > 0) {
            Logger.logTimer(ct)
            return file
        }
        return null
    }

    def postMultipart(url, Map params, File file, String contentType) {

        def result = [:]
        HTTPBuilder builder = new HTTPBuilder(url)
        builder.request(Method.POST) { request ->

            requestContentType : 'multipart/form-data'
            MultipartEntity content = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE)
            content.addPart("tilesArchive", new InputStreamBody(file.newInputStream(), contentType, file.name))
            params.each { key, value ->
                content.addPart(key, new StringBody(value?.toString()))
            }
            request.setEntity(content)

            response.success = {resp, message ->
                result.status = resp.status
                result.content = message
            }

            response.failure = {resp ->
                result.status = resp.status
                result.error = "Error POSTing to ${url}"
            }

        }
        result
    }

}
