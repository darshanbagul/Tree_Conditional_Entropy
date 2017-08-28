package pcfg_sampler_module

import pcfg_sampler_module.parsers.ParseSampler

object SampleParses {
  def main( args:Array[String] ) {
    if( !( args.length == 2 || args.length == 3 ) ) {
      println( "Must provide one argument providing the path to the grammar file and one argument for the number of parses" )
      System.exit(0)
    }

    val numParses = args(1).toInt
    val randomSeed =
      if( args.length == 3 )
        args(2).toInt
      else
        15

    val rules = GrammarReader.read( args(0) )
    val cnfRules = GrammarReader.toCNF( rules )
    val pcfg = GrammarReader.randomPCFG( cnfRules )

    val samplingParser = new ParseSampler( pcfg, randomSeed )

    io.Source.stdin.getLines.foreach{ line =>
        val s = line.split( " " )
        val parses = samplingParser.sampleParses( s, numParses )

        println( parses.mkString( "\n" ) )
    }


  }
}

