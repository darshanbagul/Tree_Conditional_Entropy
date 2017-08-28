package lin567_p2

import lin567_p2.Grammar._
import scala.annotation.tailrec

object GrammarReader {
  def read( grammarPath:String ) = {
    var rules = Map[NonTerminal,Set[RHS]]().withDefaultValue( Set() )
    var nonTerminals = Set[String]()

    def toNodeLabel( label:String ) = {
      if( nonTerminals.contains( label ) )
        NonTerminal( label )
      else
        Terminal( label )
    }

    // First, read in all the rules and identify non-terminals -- assume a symbol is a terminal if
    // it never appears on the left-hand side of a rule.
    var ruleStrings = List[Tuple2[NonTerminal,String]]()
    io.Source.fromFile( grammarPath ).getLines.foreach{ line =>
      val fields = line.split( " --> " )
      if( fields.length == 2 ) {
        val lhs = fields(0)

        nonTerminals += lhs

        // use triple double quotes so pipe isn't treated as a special character.
        fields(1).split( """ \| """ ).foreach{ rhs =>
          ruleStrings = (NonTerminal( lhs ), rhs)::ruleStrings
        }
      }
    }

    // Now that we know which node labels are non-terminals, loop back over the rule strings to
    // create the rule set.
    ruleStrings.foreach{ case ( lhs, rhs ) =>
      rules += lhs -> (
        rules(lhs) + RHS( rhs.split( " " ).map{ toNodeLabel( _ ) }.toList )
      )
    }

    rules
  }

  def toCNF(
    rules:Map[NonTerminal,Set[RHS]]
  ) = {
    val unaryTerminalIntro = unaryTerminals( rules )
    val noUnaryNonTerminal = eliminateUnitRules( unaryTerminalIntro )
    var rightBinarized = binarizeGrammar( noUnaryNonTerminal )
    rightBinarized
  }

  def unaryTerminals( rules:Map[NonTerminal,Set[RHS]] ) = {
    var newRules = Map[NonTerminal,Set[RHS]]().withDefaultValue( Set() )

    rules.foreach{ case ( lhs, rhses ) =>
      // We need to introduce new rules to generate terminals if a right-hand side includes more
      // than one node and at least one of those nodes is a terminal.
      val unaryRHSes = rhses.filter( _.length == 1 )

      // We're guaranteed for newRules(lhs) to be the empty set at this point, so we can overwrite
      // it with unaryRHSes
      newRules += lhs -> unaryRHSes

      rhses.filter( _.length > 1 ).foreach{ rhs =>
        separateTerminals( lhs, rhs.nodes ).foreach{ case ( lhs, rhses ) =>
          newRules += lhs -> ( newRules(lhs) + rhses )
        }
      }
    }

    newRules
  }

  @tailrec
  def separateTerminals(
    lhs:NonTerminal,
    originalNodes:List[Node],
    newRules:Map[NonTerminal,RHS] = Map(),
    reversedNodes:List[Node] = Nil
  ):Map[NonTerminal,RHS] = {
    if( originalNodes == Nil ) {
      newRules + ( lhs -> RHS( reversedNodes.reverse ) )
    } else {
      val thisNode = originalNodes.head
      val remaining = originalNodes.tail
      thisNode match {
        case t:Terminal => {
          // We have found a terminal node in this RHS, and need a special non-terminal label that
          // only ever introduces this terminal.
          val newNT = NonTerminal( s"W.${t.label}" )
          val newRHS = RHS( t::Nil )

          separateTerminals(
            lhs,
            remaining,
            newRules + ( newNT ->newRHS ),
            newNT::reversedNodes
          )
        }
        case _ => {
          separateTerminals( lhs, remaining, newRules, thisNode::reversedNodes )
        }
      }
    }

  }

  def findUnitRules( rules:Map[NonTerminal,Set[RHS]] ) = {
    var unitRules = Map[NonTerminal,Set[RHS]]().withDefaultValue( Set() )

    // This finds all the surface unit rules.
    rules.foreach{ case (lhs, rhses) =>
      val unitRHSes = rhses.filter( _.length == 1 )
      if( unitRHSes.size > 0 ) {
        unitRules += lhs -> unitRHSes
      }
    }


    // This finds the transitive closure of unit rules.
    var newUnitRules = unitRules
    do {
      unitRules = newUnitRules
      unitRules.foreach{ case (lhs, rhses) =>
        rhses.foreach{ rhs =>
          rhs.nodes(0) match {
            case ntRHS:NonTerminal =>
              if( unitRules.isDefinedAt( ntRHS ) ) {
                newUnitRules += lhs -> ( newUnitRules( lhs ) ++ newUnitRules( ntRHS ) )
              }
            case _ =>
          }
        }
      }
    } while( unitRules != newUnitRules )


    unitRules
  }

  def eliminateUnitRules( rules:Map[NonTerminal,Set[RHS]] ) = {
    val unitRules = findUnitRules( rules )

    // First, extract only those rules that are not unit productions.
    var newRules = rules.map{ case (lhs, rhses) => lhs -> ( rhses -- unitRules( lhs ) ) }

    // Next, extract only those rules that are unit productions that end in a terminal symbol.
    val terminalUnitRules = unitRules.map{ case ( lhs, rhses ) =>
      lhs -> rhses.collect{ _.nodes(0) match { case t:Terminal => t } }
    }

    // Finally, add terminal unit rules back in, and return.
    unitRules.foreach{ case ( lhs, rhses ) =>
      newRules += lhs -> ( newRules( lhs ) ++ rhses )
    }

    newRules
  }

  def randomPCFG( rules:Map[NonTerminal,Set[RHS]] ) = {
    val r = new util.Random( 15 )
    rules.map{ case ( lhs, rhses ) =>
      val unNormalized = Seq.fill( rhses.size )( 1 + r.nextDouble )
      val total = unNormalized.sum
      lhs -> (rhses.toSeq zip unNormalized).map{ case (rhs, unNorm ) =>
        rhs -> (unNorm/total)
      }.toMap
    }
  }

  def binarizeGrammar( rules:Map[NonTerminal,Set[RHS]] ) = {
    // First, find only the long rules.
    var longRules = Map[NonTerminal,Set[RHS]]()
    rules.foreach{ case (lhs, rhses ) =>
      longRules += lhs -> rhses.filter( _.length > 2 )
    }

    // Second, extract rules that are not too long.
    var newRules = rules.map{ case (lhs, rhses) => lhs -> ( rhses -- longRules( lhs ) ) }

    // Third, binarize those long rules.
    var rightBinarizedRules = Map[NonTerminal,Set[RHS]]().withDefaultValue( Set() )
    longRules.foreach{ case (lhs, rhses) =>
      rhses.foreach{ rhs =>
        val binarized = binarizedRHS( lhs, rhs.nodes )
        binarized.foreach{ case (binLHS, binRHSes ) =>
          rightBinarizedRules += binLHS -> ( rightBinarizedRules(binLHS) ++ binRHSes )
        }
      }
    }

    // Finally, add the binarized rules in and return.
    rightBinarizedRules.foreach{ case ( lhs, rhses ) =>
      newRules += lhs -> ( newRules.getOrElse( lhs, Set() ) ++ rhses )
    }

    newRules


  }

  @tailrec
  def binarizedRHS( lhs:NonTerminal, rhs:List[Node], accum:Map[NonTerminal,Set[RHS]] = Map() ):Map[NonTerminal,Set[RHS]] = {
    if( rhs.size == 2 ) {
      accum + ( lhs -> Set( RHS( rhs ) ) )
    } else {
      val leftChild = rhs.head
      val remaining = rhs.tail

      // We need to introduce a new non-terminal symbol for each new rule. This symbol needs to:
      //    1) not accidentally generate new strings.
      //    2) be related to the original rule so we can recover the tree from the original grammar
      //    after parsing.
      // Let's accomplish this by setting our new non-terminals to be a concatenation of the
      // following categories in the rule.
      val newLabel = NonTerminal( remaining.mkString("_") )

      binarizedRHS(
        newLabel,
        remaining,
        accum + ( lhs -> Set( RHS( List( leftChild, newLabel ) ) ) )
      )
    }
  }


}

