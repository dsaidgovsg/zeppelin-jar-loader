package zepjarloader.github

object Loader {
  import java.io._
  import java.nio.file._
  import org.apache.http.client.methods.HttpGet
  import org.apache.http.impl.client.DefaultHttpClient
  import org.apache.http.util.EntityUtils
  import org.apache.zeppelin.spark.dep.SparkDependencyContext
  import scala.util.parsing.json._

  def loadJar(
    z: SparkDependencyContext,
    repo: String,
    tag: String,
    assetName: String,
    token: Option[String],
    outputFileOrDir: String,
    readCacheFirst: Boolean = true) = {

    // Form the actual file path
    val fileType = getFileType(outputFileOrDir)

    val outputFile = fileType match {
      case FileType.File => outputFileOrDir
      case FileType.Dir => Paths.get(outputFileOrDir, assetName)
      _ => throw new Exception(f"Given output file/dir '${outputFileOrDir}' is not a valid file path or existing directory")
    }

    // Check for "cache" first
    val checkFile = new File(outputFile)

    if (!readCacheFirst || !checkFile.exists()) {
      val client = new DefaultHttpClient

      val releasesRaw = getReleases(client, repo, tag, token)
      val assetId = getAssetId(releasesRaw, assetName)
      val assetBytes = getAsset(client, repo, assetId, token)
      writeToFile(assetBytes, outputFile)

      client.getConnectionManager.shutdown
    }

    z.load(outputFile)
  }

  private object FileType extends Enumeration {
    type FileType = Value
    val File, Dir, Invalid = Value
  }

  private def getFileType(fileOrDir: String): FileType = {
    val f = new File(fileOrDir)

    if (file.isFile()) {
      FileType.File
    } else if (file.isDirectory()) {
      FileType.Dir
    } else {
      FileType.Invalid
    }
  }

  private def getReq(
    client: DefaultHttpClient,
    url: String,
    token: Option[String],
    headers: Map[String, String]): org.apache.http.HttpEntity = {
    val httpGet = new HttpGet(url)

    token match {
      case Some(token) =>
        httpGet.setHeader("Authorization", f"token ${token}")
      case _ => {}
    }

    for ((k, v) <- headers) {
      httpGet.setHeader(k, v)
    }

    val response = client.execute(httpGet)
    response.getEntity();
  }

  private def getReleases(
    client: DefaultHttpClient,
    repo: String,
    tag: String,
    token: Option[String]): String = {
    val url = f"https://api.github.com/repos/${repo}/releases/tags/${tag}"
    val entity =
      getReq(
        client,
        url,
        token,
        Map("Accept" -> "application/vnd.github.v3.raw"))
    EntityUtils.toString(entity)
  }

  private def getAsset(
    client: DefaultHttpClient,
    repo: String,
    assetId: String,
    token: Option[String]): java.io.InputStream = {
    val url = token match {
      case Some(token) =>
        f"https://${token}:@api.github.com/repos/${repo}/releases/assets/${assetId}"
      case _ =>
        f"https://api.github.com/repos/${repo}/releases/assets/${assetId}"
    }

    val entity =
      getReq(client, url, None, Map("Accept" -> "application/octet-stream"))

    entity.getContent()
  }

  private def findAsset(
    assets: List[Any],
    assetName: String): Option[Map[String, Any]] = {
    val assetsMap = assets.map(_.asInstanceOf[Map[String, Any]])
    assetsMap.find(asset => {
      val lhsAssetName = asset.get("name").get.asInstanceOf[String]
      lhsAssetName == assetName
    })
  }

  private def getAssetId(raw: String, assetName: String): String = {
    val json: Option[Any] = JSON.parseFull(raw)
    val m = json.get.asInstanceOf[Map[String, Any]]
    val assets = m.get("assets").get.asInstanceOf[List[Any]]

    if (assets.length > 1 && assetName.isEmpty) {
      throw new Exception(
        f"Asset count is ${assets.length}, unable to determine which asset to use. Please specify `assetName`")
    }

    val asset = assetName match {
      case Some(assetName) =>
        findAsset(assets, assetName) match {
          case Some(asset) => asset
          case _ =>
            throw new Exception(f"Unable to find asset name '${assetName}'")
        }
      case _ => assets(0).asInstanceOf[Map[String, Any]]
    }

    // Need to convert to value without any decimal point
    val assetId = f"${asset.get("id").get.asInstanceOf[Double]}%.0f"
    assetId
  }

  private def writeToFile(
    inputStream: java.io.InputStream,
    outputFile: String) = {
    Files.copy(
      inputStream,
      Paths.get(outputFile),
      StandardCopyOption.REPLACE_EXISTING)
  }
}

// Example usage:
// loadJar(z, "checkstyle/checkstyle", "checkstyle-8.21", Some("checkstyle-8.21-all.jar"), None /* no token */, "./checkstyle-8.21-all.jar")
