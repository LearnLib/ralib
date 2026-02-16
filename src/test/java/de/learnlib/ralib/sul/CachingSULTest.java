package de.learnlib.ralib.sul;

import static de.learnlib.ralib.example.priority.PriorityQueueSUL.DOUBLE_TYPE;
import static de.learnlib.ralib.example.priority.PriorityQueueSUL.ERROR;
import static de.learnlib.ralib.example.priority.PriorityQueueSUL.OFFER;
import static de.learnlib.ralib.example.priority.PriorityQueueSUL.OK;
import static de.learnlib.ralib.example.priority.PriorityQueueSUL.OUTPUT;
import static de.learnlib.ralib.example.priority.PriorityQueueSUL.POLL;

import java.math.BigDecimal;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.example.priority.PriorityQueueSUL;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.words.PSymbolInstance;


public class CachingSULTest {

    @Test
    public void stepTest() {
        PriorityQueueSUL sul = new PriorityQueueSUL();
        IOOracle ioOracle = new SULOracle(sul, ERROR);
        IOCache ioCache = new IOCache(ioOracle);
        CachingSUL cachingSUL = new CachingSUL(sul, ioCache);
        cachingSUL.pre();
        PSymbolInstance ok = cachingSUL.step(new PSymbolInstance(OFFER, new DataValue(DOUBLE_TYPE, new BigDecimal(1))));
        PSymbolInstance out = cachingSUL.step(new PSymbolInstance(POLL));
        cachingSUL.post();
        Assert.assertEquals(ok.getBaseSymbol(), OK);
        Assert.assertEquals(out.getBaseSymbol(), OUTPUT);
        Assert.assertEquals(sul.getInputs(), 2);
        Assert.assertEquals(sul.getResets(), 1);
    }

    @Test
    public void cachingTest() {
        PriorityQueueSUL sul = new PriorityQueueSUL();
        IOOracle ioOracle = new SULOracle(sul, ERROR);
        IOCache ioCache = new IOCache(ioOracle);
        CachingSUL cachingSUL = new CachingSUL(sul, ioCache);

        cachingSUL.pre();
        cachingSUL.step(new PSymbolInstance(OFFER, new DataValue(DOUBLE_TYPE, new BigDecimal(1))));
        cachingSUL.step(new PSymbolInstance(POLL));
        cachingSUL.post();

        Assert.assertEquals(sul.getInputs(), 2);
        Assert.assertEquals(sul.getResets(), 1);

        cachingSUL.pre();
        cachingSUL.step(new PSymbolInstance(OFFER, new DataValue(DOUBLE_TYPE, new BigDecimal(1))));
        cachingSUL.step(new PSymbolInstance(POLL));
        cachingSUL.post();

        Assert.assertEquals(sul.getInputs(), 2);
        Assert.assertEquals(sul.getResets(), 1);
    }
}
