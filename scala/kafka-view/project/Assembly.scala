import java.security.MessageDigest

import sbtassembly.AssemblyPlugin.autoImport.{PathList, assemblyJarName, assemblyMergeStrategy}
import sbtassembly.AssemblyKeys._
import sbt._
import Keys._
import sbtassembly.MergeStrategy

object Assembly {
  val aopMerge: MergeStrategy = new MergeStrategy {
    val name = "aopMerge"
    import scala.xml._
    import scala.xml.dtd._

    def apply(tempDir: File, path: String, files: Seq[File]): Either[String, Seq[(File, String)]] = {
      val dt = DocType("aspectj", PublicID("-//AspectJ//DTD//EN", "http://www.eclipse.org/aspectj/dtd/aspectj.dtd"), Nil)
      val file = MergeStrategy.createMergeTarget(tempDir, path)
      val xmls: Seq[Elem] = files.map(XML.loadFile)
      val aspectsChildren: Seq[Node] = xmls.flatMap(_ \\ "aspectj" \ "aspects" \ "_")
      val weaverChildren: Seq[Node] = xmls.flatMap(_ \\ "aspectj" \ "weaver" \ "_")
      val options: String = xmls.map(x => (x \\ "aspectj" \ "weaver" \ "@options").text).mkString(" ").trim
      val weaverAttr = if (options.isEmpty) Null else new UnprefixedAttribute("options", options, Null)
      val aspects = new Elem(null, "aspects", Null, TopScope, false, aspectsChildren: _*)
      val weaver = new Elem(null, "weaver", weaverAttr, TopScope, false, weaverChildren: _*)
      val aspectj = new Elem(null, "aspectj", Null, TopScope, false, aspects, weaver)
      XML.save(file.toString, aspectj, "UTF-8", xmlDecl = false, dt)
      IO.append(file, IO.Newline.getBytes(IO.defaultCharset))
      Right(Seq(file -> path))
    }
  }

  val mimeTypeMerge = new MergeStrategy {
    override def name: String = "mimetypemerge"

    override def apply(tempDir: File, path: String, files: Seq[File]): Either[String, Seq[(File, String)]] = {
      val lines = files flatMap (IO.readLines(_, IO.utf8)) filterNot(_.startsWith("#"))
      val unique = lines.distinct.sorted
      val file = createMergeTarget(tempDir, path)
      IO.writeLines(file, unique, IO.utf8)
      Right(Seq(file -> path))
    }

    @inline def createMergeTarget(tempDir: File, path: String): File = {
      val file = new File(tempDir, "sbtMergeTarget-" + sha1string(path) + ".tmp")
      if (file.exists) {
        IO.delete(file)
      }
      file
    }
    private def sha1 = MessageDigest.getInstance("SHA-1")
    private def sha1string(s: String): String = bytesToSha1String(s.getBytes("UTF-8"))
    private def bytesToSha1String(bytes: Array[Byte]): String =
      bytesToString(sha1.digest(bytes))
    private def bytesToString(bytes: Seq[Byte]): String =
      bytes map {"%02x".format(_)} mkString
  }

  def settings = Seq(
    test in assembly := {},
    assemblyJarName in assembly := "kafka-view.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "aop.xml") =>
        aopMerge
      case "mime.types" => mimeTypeMerge
      case "module-info.class" => MergeStrategy.discard
      case "META-INF/io.netty.versions.properties" =>
        MergeStrategy.first
      case x => MergeStrategy.defaultMergeStrategy(x)
    }
  )
}
