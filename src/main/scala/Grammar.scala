package lin567_p2.Grammar

abstract class Node {
  val label:String
  override def toString = label
}

case class NonTerminal( label:String ) extends Node
object StartNode extends NonTerminal( "--START--" )

case class Terminal( label:String ) extends Node

case class Rule( lhs:NonTerminal, rhs:RHS )
case class RHS( nodes:List[Node] ) {
  def length = nodes.length
  override def toString = nodes.mkString(" ")
}

object RHS {
  // for unary rules
  def apply( node:Node ) = new RHS( node::Nil )
  // for binary rules
  def apply( left:Node, right:Node ) = new RHS( left::right::Nil )
}

