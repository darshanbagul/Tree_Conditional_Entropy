package tree_cond_entropy

import tree_cond_entropy.parsers.CondEntParser
import tree_cond_entropy.Grammar.StartNode

object TreeConditionalEntropy {
  def main( args:Array[String] ) {
    if( args.length == 2 ) {
      println( "Must provide one argument providing the path to the grammar file" )
      System.exit(0)
    }


    val rules = GrammarReader.read( args(0) )
    val cnfRules = GrammarReader.toCNF( rules )
    val pcfg = GrammarReader.randomPCFG( cnfRules )

    val condEntParser = new CondEntParser( pcfg )

    io.Source.stdin.getLines.foreach{ line =>
      val s = line.split( " " )
      val ( stringProb, condEntropy ) = condEntParser.conditionalEntropy( s )
      println( s"P( ${line} )\t= " + stringProb )
      println( s"H( t | ${line} )\t= " + condEntropy )
    }


  }
}

