package breeze.linalg

import breeze.generic.UFunc
import breeze.macros.expand
import breeze.linalg.support.{CanTransformValues, CanMapValues, CanTraverseValues}
import breeze.linalg.support.CanTraverseValues.ValuesVisitor

object max extends UFunc {


  @expand
  @expand.valify
  implicit def maxImpl2[@expand.args(Int, Double, Float, Long) T]: Impl2[T, T, T] =
    new Impl2[T, T, T] {
      def apply(t1: T, t2: T): T = scala.math.max(t1, t2)
    }

  @expand
  @expand.valify
  implicit def maxImpl3[@expand.args(Int, Double, Float, Long) T]: Impl3[T, T, T, T] =
    new Impl3[T, T, T, T] {
      def apply(t1: T, t2: T, t3: T): T = scala.math.max( scala.math.max(t1, t2), t3 )
    }

  @expand
  implicit def reduce[T, @expand.args(Int, Double, Float, Long) S]
            (implicit iter: CanTraverseValues[T, S],
             @expand.sequence[S](Int.MinValue, Double.NegativeInfinity, Float.NegativeInfinity, Long.MinValue) init: S): Impl[T, S] = new Impl[T, S] {

    def apply(v: T): S = {
      class SumVisitor extends ValuesVisitor[S] {
        var max = init
        var visitedOne = false
        def visit(a: S): Unit = {
          visitedOne = true
          max = scala.math.max(max, a)
        }

        def zeros(numZero: Int, zeroValue: S): Unit = {
          if(numZero != 0) {
            visitedOne = true
            max = scala.math.max(zeroValue, max)
          }
        }

        override def visitArray(arr: Array[S], offset: Int, length: Int, stride: Int): Unit = {
          var i = 0
          var off = offset
          while(i < length) {
            visitedOne = true
            max = scala.math.max(max, arr(off))
            i += 1
            off += stride
          }
        }
      }

      val visit = new SumVisitor

      iter.traverse(v, visit)
      if(!visit.visitedOne) throw new IllegalArgumentException(s"No values in $v!")

      visit.max
    }

  }


  implicit def maxVS[T, U, LHS, RHS, RV](implicit cmvH: CanMapValues.HandHold[T, LHS],
                                         maxImpl: max.Impl2[LHS, RHS, LHS],
                                         cmv: CanMapValues[T, LHS, LHS, U]):Impl2[T, RHS, U] = {
    new Impl2[T, RHS, U] {
      override def apply(v: T, v2: RHS): U = cmv.map(v, maxImpl(_, v2))
    }
  }

  /**
   * Method for computing the max of the first length elements of an array. Arrays
   * of size 0 give Double.NegativeInfinity
   * @param arr
   * @param length
   * @return
   */
  def array(arr: Array[Double], length: Int) = {
    var accum = Double.NegativeInfinity
    var i = 0
    while(i < length) {
      accum = scala.math.max(arr(i), accum)
      i += 1
    }
    accum
  }

}




/**
 * Computes the minimum.
 */
object min extends UFunc {

  @expand
  @expand.valify
  implicit def minImpl2[@expand.args(Int, Double, Float, Long) T]: Impl2[T, T, T] =
  new Impl2[T, T, T] {
    def apply(t1: T, t2: T): T = scala.math.min(t1, t2)
  }

  @expand
  @expand.valify
  implicit def minImpl3[@expand.args(Int, Double, Float, Long) T]: Impl3[T, T, T, T] =
    new Impl3[T, T, T, T] {
      def apply(t1: T, t2: T, t3: T): T = scala.math.min( scala.math.min(t1, t2), t3 )
    }

  @expand
  implicit def reduce[T, @expand.args(Int, Double, Float, Long) S]
            (implicit iter: CanTraverseValues[T, S],
             @expand.sequence[S](Int.MaxValue, Double.PositiveInfinity, Float.PositiveInfinity, Long.MaxValue) init: S): Impl[T, S] = new Impl[T, S] {

    def apply(v: T): S = {
      class MinVisitor extends ValuesVisitor[S] {
        var min = init
        var visitedOne = false

        def visit(a: S): Unit = {
          visitedOne = true
          min = scala.math.min(min, a)
        }

        def zeros(numZero: Int, zeroValue: S): Unit = {
          if(numZero != 0) {
            visitedOne = true
            min = scala.math.min(zeroValue, min)
          }
        }

        override def visitArray(arr: Array[S], offset: Int, length: Int, stride: Int): Unit = {
          var i = 0
          var off = offset
          while(i < length) {
            visitedOne = true
            min = scala.math.min(min, arr(off))
            i += 1
            off += stride
          }
        }
      }

      val visit = new MinVisitor

      iter.traverse(v, visit)
      if(!visit.visitedOne) throw new IllegalArgumentException(s"No values in $v!")

      visit.min
    }

  }

  implicit def minVS[T, U, LHS, RHS, RV](implicit cmvH: CanMapValues.HandHold[T, LHS],
                                         maxImpl: min.Impl2[LHS, RHS, LHS],
                                         cmv: CanMapValues[T, LHS, LHS, U]):Impl2[T, RHS, U] = {
    new Impl2[T, RHS, U] {
      override def apply(v: T, v2: RHS): U = cmv.map(v, maxImpl(_, v2))
    }
  }
}


/**
 * clip(a, lower, upper) returns an array such that all elements are "clipped" at the range (lower, upper)
 */
object clip extends UFunc {
  implicit def clipOrdering[T,V](implicit ordering: Ordering[V], cmv: CanMapValues[T, V, V, T]):Impl3[T, V, V, T] = {
    new Impl3[T, V, V, T] {
      import ordering.mkOrderingOps
      def apply(v: T, v2: V, v3: V): T = {
        cmv.map(v, x => if(x < v2) v2 else if (x > v3) v3 else x)
      }
    }
  }

  implicit def clipInPlaceOrdering[T,V](implicit ordering: Ordering[V], cmv: CanTransformValues[T, V, V]):InPlaceImpl3[T, V, V] = {
    import ordering.mkOrderingOps
    new InPlaceImpl3[T, V, V] {
      def apply(v: T, v2: V, v3: V):Unit = {
        cmv.transform(v, x => if(x < v2) v2 else if (x > v3) v3 else x)
      }
    }
  }
}


/** Peak-to-peak, ie the Range of values (maximum - minimum) along an axis.
  */
object ptp extends UFunc {
  @expand
  implicit def reduce[T, @expand.args(Int, Double, Float, Long) S]
  (implicit iter: CanTraverseValues[T, S],
   @expand.sequence[S](Int.MaxValue, Double.PositiveInfinity, Float.PositiveInfinity, Long.MaxValue) init: S): Impl[T, S] = new Impl[T, S] {

    def apply(v: T): S = {
      class SumVisitor extends ValuesVisitor[S] {
        var max = - init
        var min = init
        var visitedOne = false

        def visit(a: S): Unit = {
          visitedOne = true
          max = scala.math.max(max, a)
          min = scala.math.min(min, a)
        }

        def zeros(numZero: Int, zeroValue: S): Unit = {
          if(numZero != 0) {
            visitedOne = true
            max = scala.math.max(zeroValue, max)
            min = scala.math.min(zeroValue, min)
          }
        }

        override def visitArray(arr: Array[S], offset: Int, length: Int, stride: Int): Unit = {
          var i = 0
          var off = offset
          while(i < length) {
            visitedOne = true
            max = scala.math.max(max, arr(off))
            min = scala.math.min(min, arr(off))
            i += 1
            off += stride
          }
        }
      }

      val visit = new SumVisitor

      iter.traverse(v, visit)
      if(!visit.visitedOne) throw new IllegalArgumentException(s"No values in $v!")

      visit.max - visit.min
    }

  }
}

/** Minimum and maximum in one traversal, along an axis.
  */
object minMax extends UFunc {
  @expand
  implicit def reduce[T, @expand.args(Int, Double, Float, Long) S]
  (implicit iter: CanTraverseValues[T, S],
   @expand.sequence[S](Int.MaxValue, Double.PositiveInfinity, Float.PositiveInfinity, Long.MaxValue) init: S): Impl[T, (S, S)] = new Impl[T, (S, S)] {

    def apply(v: T): (S, S) = {
      //the next 30 lines are common with "object ptp extends UFunc"
      class SumVisitor extends ValuesVisitor[S] {
        var max = - init
        var min = init
        var visitedOne = false
        def visit(a: S): Unit = {
          visitedOne = true
          max = scala.math.max(max, a)
          min = scala.math.min(min, a)
        }

        def zeros(numZero: Int, zeroValue: S): Unit = {
          if(numZero != 0) {
            visitedOne = true
            max = scala.math.max(zeroValue, max)
            min = scala.math.min(zeroValue, min)
          }
        }

        override def visitArray(arr: Array[S], offset: Int, length: Int, stride: Int): Unit = {
          var i = 0
          var off = offset
          while(i < length) {
            visitedOne = true
            max = scala.math.max(max, arr(off))
            min = scala.math.min(min, arr(off))
            i += 1
            off += stride
          }
        }
      }

      val visit = new SumVisitor

      iter.traverse(v, visit)
      if(!visit.visitedOne) throw new IllegalArgumentException(s"No values in $v!")

      (visit.min, visit.max)

    }

  }

}
