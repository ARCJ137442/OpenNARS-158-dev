package nars.language

import java.util._
import nars.io.Symbols
import nars.storage.Memory
import IntersectionInt._
//remove if not needed
import scala.collection.JavaConversions._
import CompoundTerm._

object IntersectionInt {

  /**
   * Try to make a new compound from two components. Called by the inference rules.
   * @param term1 The first compoment
   * @param term2 The first compoment
   * @param memory Reference to the memory
   * @return A compound generated or a term it reduced to
   */
  def make(term1: Term, term2: Term, memory: Memory): Term = {
    var set: TreeSet[Term] = null
    if ((term1.isInstanceOf[SetExt]) && (term2.isInstanceOf[SetExt])) {
      set = new TreeSet[Term](term1.asInstanceOf[CompoundTerm].cloneComponents())
      set.addAll(term2.asInstanceOf[CompoundTerm].cloneComponents())
      return SetExt.make(set, memory)
    }
    if ((term1.isInstanceOf[SetInt]) && (term2.isInstanceOf[SetInt])) {
      set = new TreeSet[Term](term1.asInstanceOf[CompoundTerm].cloneComponents())
      set.retainAll(term2.asInstanceOf[CompoundTerm].cloneComponents())
      return SetInt.make(set, memory)
    }
    if (term1.isInstanceOf[IntersectionInt]) {
      set = new TreeSet[Term](term1.asInstanceOf[CompoundTerm].cloneComponents())
      if (term2.isInstanceOf[IntersectionInt]) {
        set.addAll(term2.asInstanceOf[CompoundTerm].cloneComponents())
      } else {
        set.add(term2.clone().asInstanceOf[Term])
      }
    } else if (term2.isInstanceOf[IntersectionInt]) {
      set = new TreeSet[Term](term2.asInstanceOf[CompoundTerm].cloneComponents())
      set.add(term1.clone().asInstanceOf[Term])
    } else {
      set = new TreeSet[Term]()
      set.add(term1.clone().asInstanceOf[Term])
      set.add(term2.clone().asInstanceOf[Term])
    }
    make(set, memory)
  }

  /**
   * Try to make a new IntersectionExt. Called by StringParser.
   * @return the Term generated from the arguments
   * @param argList The list of components
   * @param memory Reference to the memory
   */
  def make(argList: ArrayList[Term], memory: Memory): Term = {
    val set = new TreeSet[Term](argList)
    make(set, memory)
  }

  /**
   * Try to make a new compound from a set of components. Called by the public make methods.
   * @param set a set of Term as compoments
   * @param memory Reference to the memory
   * @return the Term generated from the arguments
   */
  def make(set: TreeSet[Term], memory: Memory): Term = {
    if (set.size == 1) {
      return set.first()
    }
    val argument = new ArrayList[Term](set)
    val name = makeCompoundName(Symbols.INTERSECTION_INT_OPERATOR, argument)
    val t = memory.nameToListedTerm(name)
    if ((t != null)) t else new IntersectionInt(argument)
  }
}

/**
 * A compound term whose intension is the intersection of the extensions of its components
 */
class IntersectionInt private (arg: ArrayList[Term]) extends CompoundTerm(arg) {

  /**
   * Constructor with full values, called by clone
   * @param n The name of the term
   * @param cs Component list
   * @param open Open variable list
   * @param i Syntactic complexity of the compound
   */
  private def this(n: String, 
      cs: ArrayList[Term], 
      con: Boolean, 
      i: Short) {
//    super(n, cs, con, i)
    this(cs)
    setName(n)
    this.isConstant_ = con
    this.complexity = i
  }

  /**
   * Clone an object
   * @return A new object, to be casted into a Conjunction
   */
  override def clone(): AnyRef = {
    new IntersectionInt(name, cloneList(components).asInstanceOf[ArrayList[Term]], isConstant_, complexity)
  }

  /**
   * Get the operator of the term.
   * @return the operator of the term
   */
  def operator(): String = Symbols.INTERSECTION_INT_OPERATOR

  /**
   * Check if the compound is communitative.
   * @return true for communitative
   */
  override def isCommutative(): Boolean = true
}
