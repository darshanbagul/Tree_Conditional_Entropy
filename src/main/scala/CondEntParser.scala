package tree_cond_entropy.parsers

import tree_cond_entropy.Grammar._

class CondEntParser( pcfg:Map[NonTerminal,Map[RHS,Double]] ) {

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
  var condEntChart = Array.fill( maxLength, maxLength )( Map[NonTerminal,Double]().withDefaultValue(0D) )

  def lexFill( word:String, i:Int ) {
    val rhs = RHS( Terminal( word ) )
    reversePCFG.getOrElse( rhs, Map() ).foreach{ case ( pos, prob ) =>
      insideChart( i )( i+1 ) += pos -> prob
      condEntChart( i )( i+1 ) += pos -> 0D
    }
  }

  def log2( x:Double ) = math.log( x ) / math.log( 2 )

  def synFill( i:Int, j:Int ) {
    ( (i+1) to (j-1) ).foreach{ k =>
      val leftChildren = insideChart( i )( k )
      val rightChildren = insideChart( k )( j )

      leftChildren.foreach{ case ( lNode, lProb ) =>
        rightChildren.foreach{ case ( rNode, rProb ) =>
          val rhs = RHS( lNode, rNode )

          reversePCFG.getOrElse( rhs, Map() ).foreach{ case ( pNode, ruleProb ) =>

            val parentIncrement = ruleProb * lProb * rProb

            insideChart( i )( j ) += pNode -> (
              insideChart( i )( j )( pNode ) + ( 
                parentIncrement
              )
            )

          }
        }
      }
    }
  }

  def condEntSyn( i:Int, j:Int ) {

    insideChart( i )( j ).foreach{ case (pNode, pProb) =>

      val rhses = pcfg( pNode )

      ( i+1 to j-1 ).foreach{ k =>
        val leftChildren = insideChart( i )( k )
        val rightChildren = insideChart( k )( j )

        leftChildren.foreach{ case ( lNode, lProb ) =>
          rightChildren.foreach{ case ( rNode, rProb ) =>

            val rhs = RHS( lNode, rNode )

            if( rhses.contains( rhs ) ) {

              val ruleProb = rhses( rhs )

              val temp = lProb * rProb * ruleProb / insideChart( i )( j )( pNode )

              // Implement me
              val splitScore = temp * -1 * log2(temp)
              val condEntIncrement =  splitScore  + temp * condEntChart( i )( k )(lNode) + temp * condEntChart( k )( j )(rNode)
              
              condEntChart( i )( j ) += pNode -> (
                condEntChart( i )( j )( pNode ) + ( 
                  condEntIncrement
                )
              )

            }

          }
        }
      }
    }
  }

def condEntSyn( i:Int, j:Int ) {

    insideChart( i )( j ).foreach{ case (pNode, pProb) =>

      val rhses = pcfg( pNode )

      ( i+1 to j-1 ).foreach{ k =>
        val leftChildren = insideChart( i )( k )
        val rightChildren = insideChart( k )( j )

        leftChildren.foreach{ case ( lNode, lProb ) =>
          rightChildren.foreach{ case ( rNode, rProb ) =>

            val rhs = RHS( lNode, rNode )

            if( rhses.contains( rhs ) ) {

              val ruleProb = rhses( rhs )

              val splitScore = insideChart( i )( k )( lNode ) * 
                               insideChart( k )( j )( rNode ) * 
                               pcfg( pNode )( RHS( lNode, rNode ) ) / 
                               insideChart( i )( j )( pNode )

              // Implement me
              val condEntIncrement =  (splitScore * -1 * log2(splitScore)) + 
                                      (splitScore * condEntChart( i )( k )(lNode)) + 
                                      (splitScore * condEntChart( k )( j )(rNode))

              condEntChart( i )( j ) += pNode -> (
                condEntChart( i )( j )( pNode ) + (
                  condEntIncrement
                )
              )

            }

          }
        }
      }
    }
  }

  def initializeChart( length:Int ) {
    insideChart = Array.fill( maxLength, maxLength )( Map[NonTerminal,Double]().withDefaultValue(0) )
    condEntChart = Array.fill( maxLength, maxLength )( Map[NonTerminal,Double]().withDefaultValue(0) )
  }

  def insidePass( s:Array[String] ) {
    // first clean the chart
    initializeChart( s.length )

    (1 to s.length).foreach{ j =>
      lexFill( s( j-1 ), j-1 )


      if( j > 1 ) {
        (0 to (j-2)).reverse.foreach{ i =>
          synFill( i, j )
          condEntSyn( i, j )
        }
      }
    }

  }

  def conditionalEntropy( s:Array[String] ) = {
    insidePass( s )

    ( insideChart( 0 )( s.length )( NonTerminal( "S" ) ),
      condEntChart( 0 )( s.length )( NonTerminal( "S" ) )
    )
  }

}

