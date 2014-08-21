package contege

import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.ArrayList
import java.lang.reflect.Constructor
import contege.seqgen._

/**
 * Computes all sequential interleavings of two suffixes.
 * Both suffixes must share the same prefix.
 */
class SequentialInterleavings(prefix: Prefix, suffix1: Suffix, suffix2: Suffix) {
    assert(suffix1.prefix == suffix2.prefix && suffix1.prefix == prefix)

    // compute boolean interleavings (relatively cheap)
    private val booleanInterleavings = new ArrayList[Array[Boolean]]()
    private val emptySlots = new Array[Boolean](suffix1.length + suffix2.length)
    interleavings(emptySlots, suffix2.length, -1)

    // iterator to create real interleavings (i.e. complete call sequences) on demand
    private val booleanInterleavingsIter = booleanInterleavings.iterator

    def nextInterleaving: Option[Prefix] = {
        if (!booleanInterleavingsIter.hasNext) return None
        val booleanInterleaving = booleanInterleavingsIter.next
        assert(booleanInterleaving.length == suffix1.length + suffix2.length)
        val interleaved = prefix.copy
        val oldVar2NewVarSuffix1 = Map[Variable, Variable]()
        val oldVar2NewVarSuffix2 = Map[Variable, Variable]()
        val callIter1 = suffix1.callIterator
        val callIter2 = suffix2.callIterator
        booleanInterleaving.foreach(b => {
            val call = if (!b) callIter1.next else callIter2.next
            // create new vars for ret vals in suffixes to avoid redefs in interleaving
            val newRetVal = if (call.retVal.isDefined) {
                val newVar = new ObjectVariable
                if (!b) oldVar2NewVarSuffix1.put(call.retVal.get, newVar) else oldVar2NewVarSuffix2.put(call.retVal.get, newVar) 
            	Some(newVar)
            } else call.retVal
            val newReceiver = if (call.receiver.isDefined) {
            	Some((if (!b) oldVar2NewVarSuffix1 else oldVar2NewVarSuffix2).getOrElse(call.receiver.get, call.receiver.get))
            } else call.receiver
            val newArgs = new ArrayList[Variable]
            call.args.foreach(arg => {
                val newArg = (if (!b) oldVar2NewVarSuffix1 else oldVar2NewVarSuffix2).getOrElse(arg, arg)
                newArgs.add(newArg)
            })
            interleaved.appendCall(call.atom, newReceiver, newArgs, newRetVal)
        })
        assert(!callIter1.hasNext && !callIter2.hasNext)
        assert(interleaved.length == suffix1.length + suffix2.length + prefix.length)
        Some(interleaved)
    }

    private def interleavings(t2sSlots: Array[Boolean], remainingForT2: Int, lastForT2: Int): Unit = {
        if (lastForT2 > t2sSlots.size - 1 - remainingForT2) return

        if (remainingForT2 == 0) {
            booleanInterleavings.add(t2sSlots)
            return
        }

        for (newForT2 <- lastForT2 + 1 to t2sSlots.size - 1) {
            val slotsCopy = t2sSlots.clone
            slotsCopy(newForT2) = true
            interleavings(slotsCopy, remainingForT2 - 1, newForT2)
        }
    }

}

	