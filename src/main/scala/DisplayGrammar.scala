package lin567_p2

object DisplayGrammar {
  def main( args:Array[String] ) {
    if( args.length != 1 ) {
      println( "Must provide one argument providing the path to the grammar file" )
      System.exit(0)
    }

    val rules = GrammarReader.read( args(0) )

    rules.foreach{ case (lhs, rhses) =>
      println( rhses.mkString( s"$lhs --> ", s"\n$lhs --> ", "\n" ) )
    }

    val randomCNF = GrammarReader.randomPCFG( GrammarReader.toCNF( rules ) )
    println( "---\nFully pipelined" )
    randomCNF.foreach{ case (lhs, rhses) =>
      println( rhses.map{rhs => rhs._1 + "\t" + rhs._2}.mkString( s"$lhs --> ", s"\n$lhs --> ", "\n" ) )
    }

  }


}

