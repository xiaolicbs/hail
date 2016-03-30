package org.broadinstitute.hail.driver

import org.broadinstitute.hail.Utils._
import org.broadinstitute.hail.expr._
import org.broadinstitute.hail.io.annotators._
import org.kohsuke.args4j.{Option => Args4jOption}

object AnnotateVariantsTSV extends Command {

  class Options extends BaseOptions {
    @Args4jOption(required = true, name = "-i", aliases = Array("--input"),
      usage = "TSV file path")
    var condition: String = _

    @Args4jOption(required = false, name = "-t", aliases = Array("--types"),
      usage = "Define types of fields in annotations files")
    var types: String = ""

    @Args4jOption(required = true, name = "-r", aliases = Array("--root"),
      usage = "Period-delimited path starting with `va'")
    var root: String = _

    @Args4jOption(required = false, name = "-m", aliases = Array("--missing"),
      usage = "Specify identifier to be treated as missing")
    var missingIdentifier: String = "NA"

    @Args4jOption(required = false, name = "-v", aliases = Array("--vcolumns"),
      usage = "Specify the column identifiers for chromosome, position, ref, and alt (in that order)")
    var vCols: String = "Chromosome,Position,Ref,Alt"
  }

  def newOptions = new Options

  def name = "annotatevariants tsv"

  def description = "Annotate variants with TSV file"

  def parseColumns(s: String): Array[String] = {
    val split = s.split(",").map(_.trim)
    fatalIf(split.length != 4 && split.length != 1,
      s"""Cannot read chr, pos, ref, alt columns from `$s':
          |  enter four comma-separated column identifiers for separate chr/pos/ref/alt columns, or
          |  one column identifier for a single chr:pos:ref:alt column.""".stripMargin)
    split
  }

  def parseRoot(s: String): List[String] = {
    val split = s.split("\\.").toList
    fatalIf(split.isEmpty || split.head != "va", s"Root must start with `va.', got `$s'")
    split.tail
  }

  def run(state: State, options: Options): State = {
    val vds = state.vds
    val (rdd, signature) = VariantTSVAnnotator(vds.sparkContext, options.condition,
      parseColumns(options.vCols),
      Parser.parseAnnotationTypes(options.types),
      options.missingIdentifier)
    val annotated = vds.annotateVariants(rdd, signature, parseRoot(options.root))

    state.copy(vds = annotated)
  }
}