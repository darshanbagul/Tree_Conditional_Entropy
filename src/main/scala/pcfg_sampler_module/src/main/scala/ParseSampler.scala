package pcfg_sampler_module.parsers

import pcfg_sampler_module.Grammar._

class ParseSampler( pcfg:Map[NonTerminal,Map[RHS,Double]], randomSeed:Int = 15 ) {

  // For convenience, we want a reversal of the rules map; given an expansion, what are the possible
  // dominating non-terminals?
  var reversePCFG = Map[RHS,Map[NonTerminal,Double]]()

  pcfg.foreach{ case ( lhs, rhses ) =>
    rhses.foreach{ case ( rhs, prob ) =>
      if( reversePCFG.isDefinedAt( rhs ) ) {
        reversePCFG += rhs -> ( reversePCFG( rhs ) + ( lhs -> prob ) )
      } else {
        reversePCFG += rhs -> Map( lhs -> prob )
      }
    }
  }

  val maxLength = 20

  // Arrays are always mutable!
  var insideChart = Array.fill( maxLength, maxLength )( Map[NonTerminal,Double]().withDefaultValue(0D) )

  // Backtraces records the set of possible splits and left and right children for each non-terminal
  var parseBackTraces = Array.fill( maxLength, maxLength )(
    Map[NonTerminal,Set[Tuple3[Int,NonTerminal,NonTerminal]]]().withDefaultValue(Set())
  )

  def lexFill( word:String, i:Int ) {
    val rhs = RHS( Terminal( word ) )
    reversePCFG.getOrElse( rhs, Map() ).foreach{ case ( pos, prob ) =>
      insideChart( i )( i+1 ) += pos -> prob
    }
  }

  def synFill( i:Int, j:Int ) {
    ( (i+1) to (j-1) ).foreach{ k =>
      val leftChildren = insideChart( i )( k )
      val rightChildren = insideChart( k )( j )

      leftChildren.foreach{ case ( lNode, lProb ) =>
        rightChildren.foreach{ case ( rNode, rProb ) =>
          val rhs = RHS( lNode, rNode )


          reversePCFG.getOrElse( rhs, Map() ).foreach{ case ( pNode, ruleProb ) =>

            // implement me!
            val parentIncrement = lProb * rProb * ruleProb

            insideChart( i )( j ) += pNode -> (
              insideChart( i )( j )( pNode ) + ( 
                parentIncrement
              )
            )

            // implement me!
            val split = Tuple3( k, NonTerminal( lNode.toString), NonTerminal( rNode.toString ) )
            parseBackTraces( i )( j ) += pNode -> (
              parseBackTraces( i )( j )( pNode ) + split
            )
          }
        }
      }
    }
  }


  def initializeChart( length:Int ) {
    insideChart = Array.fill( maxLength, maxLength+1 )( Map[NonTerminal,Double]().withDefaultValue(0) )
    parseBackTraces = Array.fill( maxLength, maxLength )(
      Map[NonTerminal,Set[Tuple3[Int,NonTerminal,NonTerminal]]]().withDefaultValue(Set())
    )
  }

  def insidePass( s:Array[String] ) {
    // first clean the chart
    initializeChart( s.length )

    (1 to s.length).foreach{ j =>
      lexFill( s( j-1 ), j-1 )


      if( j > 1 ) {
        (0 to (j-2)).reverse.foreach{ i =>
          synFill( i, j )
        }
      }
    }

  }

  val rand = new util.Random( randomSeed )

  // This implements the function Sample in Johnson et al. (2007).
  // A more clever implementation would take advantage of tail recursion for speed and memory gains
  // (see separateTerminals and binarizedRHS in GrammarReader.scala), but this implementation will
  // do for the homework.
  def sampleTree( s:Array[String], i:Int, j:Int, pNode:NonTerminal ):String = {
    if( j-i == 1 ) {
      "(" + pNode + " " + s(i) + ")"
    } else {
      var randomSplit:Tuple3[Int,NonTerminal,NonTerminal] = null
      var random = rand.nextDouble

      var runningTotal = 0D

      parseBackTraces( i )( j )( pNode ).foreach{ split =>
        val ( k, lNode, rNode ) = split

        // implement me
        val splitScore = (pcfg.get(pNode).get(RHS(lNode, rNode)).get * insideChart(i)(k).get(NonTerminal(lNode.toString)).get * insideChart(k)(j).get(NonTerminal(rNode.toString)).get)/ insideChart(i)(j).get(pNode).get

        if( runningTotal < random && runningTotal + splitScore > random ) {
          randomSplit = split
        }
        runningTotal += splitScore
      }

      val ( sampledK, sampledLeft, sampledRight ) = randomSplit

      "(" + pNode + " " + sampleTree( s, i, sampledK, sampledLeft ) + " " +
        sampleTree( s, sampledK, j, sampledRight ) + " )"

    }
  }

  // Takes as input a sequence of words and the number of parses to sample, returns a list of
  // sampled parses.
  def sampleParses( s:Array[String], numParses:Int ) = {
    insidePass( s )
    (0 until numParses).map{ _ =>
      sampleTree( s, 0, s.length, NonTerminal( "S" ) )
    }
  }



}

