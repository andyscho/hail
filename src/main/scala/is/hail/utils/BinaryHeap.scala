package is.hail.utils

import scala.collection.mutable
import scala.reflect.ClassTag

class BinaryHeap[@specialized T: ClassTag](minimumCapacity: Int = 32, maybeTieBreaker: (T, T) => Int = null) {
  private var ts: Array[T] = new Array[T](minimumCapacity)
  private var ranks: Array[Long] = new Array[Long](minimumCapacity)
  private val m: mutable.Map[T, Int] = new mutable.HashMap()
  private var next: Int = 0

  private def isLeftFavoredTie(ai: Int, bi: Int): Boolean =
    maybeTieBreaker != null && ranks(ai) == ranks(bi) && maybeTieBreaker(ts(ai), ts(bi)) > 0

  def size: Int = next

  def isEmpty: Boolean = next == 0
  def nonEmpty: Boolean = next != 0

  override def toString(): String =
    s"values: ${ ts.slice(0, next): IndexedSeq[T] }; ranks: ${ ranks.slice(0, next): IndexedSeq[Long] }"

  def insert(t: T, r: Long) {
    if (m.contains(t))
      throw new RuntimeException(s"key $t already exists with priority ${ ranks(m(t)) }, cannot add it again with priority $r")
    maybeGrow()
    put(next, t, r)
    bubbleUp(next)
    next += 1
  }

  private def emptyHeap() = new RuntimeException("heap is empty")

  def max(): T =
    if (next > 0)
      ts(0)
    else
      throw emptyHeap()

  def maxPriority(): Long =
    if (next > 0)
      ranks(0)
    else
      throw emptyHeap()

  def extractMax(): T = {
    val max = if (next > 0)
      ts(0)
    else
      throw emptyHeap()

    next -= 1
    m -= max
    if (next > 0) {
      put(0, ts(next), ranks(next))
      bubbleDown(0)
      maybeShrink()
    }
    max
  }

  def getPriority(t: T): Long =
    ranks(m(t))

  def decreasePriorityTo(t: T, r: Long) {
    val i = m(t)
    assert(ranks(i) > r)
    ranks(i) = r
    bubbleDown(i)
  }

  def decreasePriority(t: T, f: (Long) => Long) {
    val i = m(t)
    val r = f(ranks(i))
    assert(ranks(i) > r)
    ranks(i) = r
    bubbleDown(i)
  }

  def increasePriorityTo(t: T, r: Long) {
    val i = m(t)
    assert(ranks(i) < r)
    ranks(i) = r
    bubbleUp(i)
  }

  def increasePriority(t: T, f: (Long) => Long) {
    val i = m(t)
    val r = f(ranks(i))
    assert(ranks(i) < r)
    ranks(i) = r
    bubbleUp(i)
  }

  def contains(t: T): Boolean =
    m.contains(t)

  def toArray: Array[T] = {
    val trimmed = new Array(size)
    Array.copy(ts, 0, trimmed, 0, next)
    trimmed
  }

  private def parent(i: Int) =
    if (i == 0) 0 else (i - 1) >>> 1

  private def put(to: Int, t: T, rank: Long) {
    ts(to) = t
    ranks(to) = rank
    m(t) = to
  }

  private def swap(i: Int, j: Int) {
    val tempt = ts(i)
    ts(i) = ts(j)
    ts(j) = tempt
    val temprank = ranks(i)
    ranks(i) = ranks(j)
    ranks(j) = temprank
    m(ts(j)) = j
    m(ts(i)) = i
  }

  private def maybeGrow() {
    if (next >= ts.length) {
      val ts2 = new Array[T](ts.length << 1)
      val ranks2 = new Array[Long](ts.length << 1)
      Array.copy(ts, 0, ts2, 0, ts.length)
      Array.copy(ranks, 0, ranks2, 0, ts.length)
      ts = ts2
      ranks = ranks2
    }
  }

  private def maybeShrink() {
    if (next >= minimumCapacity && next < (ts.length >>> 2)) {
      val ts2 = new Array[T](ts.length >>> 2)
      val ranks2 = new Array[Long](ts.length >>> 2)
      Array.copy(ts, 0, ts2, 0, ts2.length)
      Array.copy(ranks, 0, ranks2, 0, ts2.length)
      ts = ts2
      ranks = ranks2
    }
  }

  private def bubbleUp(i: Int) {
    var current = i
    var p = parent(current)
    while (ranks(current) > ranks(p) || isLeftFavoredTie(current, p)) {
      swap(current, p)
      current = p
      p = parent(current)
    }
  }

  private def bubbleDown(i: Int) {
    var current = i
    var largest = current
    var continue = false
    do {
      val leftChild = (current << 1) + 1
      val rightChild = (current << 1) + 2

      if (leftChild < next && (ranks(leftChild) > ranks(largest) || isLeftFavoredTie(leftChild, largest)))
        largest = leftChild
      if (rightChild < next && (ranks(rightChild) > ranks(largest) || isLeftFavoredTie(rightChild, largest)))
        largest = rightChild

      if (largest != current) {
        swap(largest, current)
        current = largest
        continue = true
      } else
        continue = false
    } while (continue)
  }

  def checkHeapProperty() {
    checkHeapProperty(0)
  }

  private def checkHeapProperty(current: Int) {
    val leftChild = (current << 1) + 1
    val rightChild = (current << 1) + 2
    if (leftChild < next) {
      assertHeapProperty(leftChild, current)
      if (rightChild < next) {
        assertHeapProperty(rightChild, current)
        checkHeapProperty(rightChild)
      }
      checkHeapProperty(leftChild)
    } else {
      if (rightChild < next) {
        assertHeapProperty(rightChild, current)
        checkHeapProperty(rightChild)
      }
    }
  }

  private def assertHeapProperty(child: Int, parent: Int) {
    assert(ranks(child) <= ranks(parent),
      s"heap property violated at parent $parent, child $child: ${ ts(parent) }:${ ranks(parent) } < ${ ts(child) }:${ ranks(child) }")
  }

}
