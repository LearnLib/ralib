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
        CachingSUL cachingSul = new CachingSUL(sul, ioCache);
        cachingSul.pre();
        PSymbolInstance ok = cachingSul.step(new PSymbolInstance(OFFER, new DataValue<BigDecimal>(DOUBLE_TYPE, new BigDecimal(1))));
        PSymbolInstance out = cachingSul.step(new PSymbolInstance(POLL));
        cachingSul.post();
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
        CachingSUL cachingSul = new CachingSUL(sul, ioCache);


        cachingSul.pre();
        cachingSul.step(new PSymbolInstance(OFFER, new DataValue<BigDecimal>(DOUBLE_TYPE, new BigDecimal(1))));
        cachingSul.step(new PSymbolInstance(POLL));
        cachingSul.post();

        Assert.assertEquals(sul.getInputs(), 2);
        Assert.assertEquals(sul.getResets(), 1);

        cachingSul.pre();
        cachingSul.step(new PSymbolInstance(OFFER, new DataValue<BigDecimal>(DOUBLE_TYPE, new BigDecimal(1))));
        cachingSul.step(new PSymbolInstance(POLL));
        cachingSul.post();

        Assert.assertEquals(2, sul.getInputs());
        Assert.assertEquals(1, sul.getResets());
    }
}
