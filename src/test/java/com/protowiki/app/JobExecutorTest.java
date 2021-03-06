package com.protowiki.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Nick
 */
@Ignore
public class JobExecutorTest {

    public static Logger logger = LoggerFactory.getLogger(JobExecutorTest.class);

    @Rule
    public TestName testName = new TestName();

    public JobExecutorTest() {
    }

    @Before
    public void setUp() {
        logger.debug("before: {}", testName.getMethodName());
    }

    @After
    public void tearDown() {
        logger.debug("after: {}", testName.getMethodName());
    }

    /**
     * Test of submitJob method, of class JobExecutor.
     */
    @Test
    public void testSubmitJob() {

        Callable<String> task = (Callable<String>) () -> {
            return "test callback";
        };

        Future<?> future = JobExecutorService.submitJob(task);
        String result = null;
        try {
            result = (String) future.get();
        } catch (InterruptedException | ExecutionException ex) {
            logger.error("Exception", ex);
        }
        logger.info("result: {}", result);
    }

    /**
     * Test of submitMultipleJobs method, of class JobExecutor.
     */
    @Test
    public void testSubmitMultipleJobs() {

        List<Callable<?>> tasks = batchJobs();

        List<Future<?>> result = JobExecutorService.submitMultipleJobs(tasks);
        result.stream().forEach(future -> {
            try {
                String str = (String) future.get();
                logger.info("output: {}", str);
            } catch (InterruptedException | ExecutionException ex) {
                logger.error("Exception", ex);
            }
        });
        logger.info("done.");
    }
    
    @Test
    public void testCompletionService() {
        
    }
    
    void solver1(Executor e, Collection<Callable<String>> solvers) throws InterruptedException, ExecutionException {
        
        CompletionService<String> ecs = new ExecutorCompletionService<>(e);
        solvers.stream().forEach((s) -> {
            ecs.submit(s);
        });
        for (int i=0; i<solvers.size(); i++) {
            String s = ecs.take().get();
            if (s!=null) {
                // use(s);
            }
        }
    }
    void solver2(Executor e, Collection<Callable<String>> solvers) throws InterruptedException, ExecutionException {
        
        CompletionService<String> ecs = new ExecutorCompletionService<>(e);
        int n = solvers.size();
        List<Future<String>> futures = new ArrayList<>(n);
        String result = null;
        try {
            solvers.stream().forEach((s) -> {
                futures.add(ecs.submit(s));
            });
            for (int i=0; i<n; ++i) {
                try {
                    String r = ecs.take().get();
                    if (r!=null) {
                        result = r;
                        break;
                    }
                } catch (ExecutionException ignore) {}
            }
        } finally {
            futures.stream().forEach((f) -> {
                f.cancel(true);
            });
        }
        if (result !=null) {
            // use(result);
        }
    }

    public List<Callable<?>> batchJobs() {
        return IntStream.range(0, 50).boxed().map(i -> {
            return (Callable<String>) () -> {
                TimeUnit.SECONDS.sleep(3);
                return "job " + i + "  has finished";
            };
        }).collect(Collectors.toList());
    }

}
