/**
 * Created by mattia on 27.02.15.
 */

import collection.mutable.Stack
import org.scalatest._

class CollectDecideDataset_test extends FunSuite{

  test("Stack con due elementi"){
    val stack = new Stack[Int]
    stack.push(1)
    stack.push(2)
    assert(stack.pop() === 2)
    assert(stack.pop() === 1)
  }
}
