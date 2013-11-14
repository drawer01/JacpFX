package org.jacp.test.lifesycleannotations;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import junit.framework.Assert;
import org.jacp.api.component.IPerspective;
import org.jacp.api.component.ISubComponent;
import org.jacp.api.component.Injectable;
import org.jacp.javafx.rcp.workbench.AFXWorkbench;
import org.jacp.test.AllTests;
import org.jacp.test.components.ComponentShutdownAndRestartComponentsTests1;
import org.jacp.test.main.ApplicationPredestroyPerspectiveTest;
import org.jacp.test.main.ApplicationShutdownAndRestartComponentsTest;
import org.jacp.test.perspectives.PerspectiveOnePredestroyPerspectiveTest;
import org.jacp.test.perspectives.PerspectiveShutdownAndRestartComponents;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: Andy Moncsek
 * Date: 13.11.13
 * Time: 21:37
 * To change this template use File | Settings | File Templates.
 */
public class ShutdownAndReopenComponentsTest {

    static Thread t;

    @AfterClass
    public static void exitWorkBench() {
        Platform.exit();
        AllTests.resetApplication();


    }

    @BeforeClass
    public static void initWorkbench() {


        t = new Thread("JavaFX Init Thread") {
            public void run() {

                ApplicationShutdownAndRestartComponentsTest.main(new String[0]);

            }
        };
        t.setDaemon(true);
        t.start();
        // Pause briefly to give FX a chance to start
        try {
            ApplicationShutdownAndRestartComponentsTest.latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    /**
     * check if component is active
     */
    public void test1() {

        ApplicationShutdownAndRestartComponentsTest launcher = ApplicationShutdownAndRestartComponentsTest.instance[0];
        AFXWorkbench workbench = launcher.getWorkbench();
        assertNotNull(workbench);
        List<IPerspective<EventHandler<Event>, Event, Object>> perspectives = workbench.getPerspectives();
        assertNotNull(perspectives);
        assertFalse(perspectives.isEmpty());

        for(IPerspective<EventHandler<Event>, Event, Object> p:perspectives) {

            assertTrue(p.getContext().isActive());
            List<ISubComponent<EventHandler<Event>, Event, Object>> components = p.getSubcomponents();
            assertFalse(components.isEmpty());
            components.forEach(c-> assertTrue(c.getContext().isActive()));
        }
    }
    @Test
    /**
     * Test shutDownComponent
     */
    public void test2() throws InterruptedException {
        // Component is active
        ApplicationShutdownAndRestartComponentsTest launcher = ApplicationShutdownAndRestartComponentsTest.instance[0];
        AFXWorkbench workbench = launcher.getWorkbench();
        assertNotNull(workbench);
        List<IPerspective<EventHandler<Event>, Event, Object>> perspectives = workbench.getPerspectives();
        assertNotNull(perspectives);
        assertFalse(perspectives.isEmpty());
        for(IPerspective<EventHandler<Event>, Event, Object> p:perspectives) {

            assertTrue(p.getContext().isActive());
            List<ISubComponent<EventHandler<Event>, Event, Object>> components = p.getSubcomponents();
            assertFalse(components.isEmpty());

            components.forEach(c-> assertTrue(c.getContext().isActive()));
        }

        testStopComponent();


    }


    private void testStopComponent() throws InterruptedException {
        ApplicationShutdownAndRestartComponentsTest launcher = ApplicationShutdownAndRestartComponentsTest.instance[0];
        AFXWorkbench workbench = launcher.getWorkbench();
        assertNotNull(workbench);
        List<IPerspective<EventHandler<Event>, Event, Object>> perspectives = workbench.getPerspectives();
        assertNotNull(perspectives);
        assertFalse(perspectives.isEmpty());
        ComponentShutdownAndRestartComponentsTests1.stopLatch = new CountDownLatch(1);

        PerspectiveShutdownAndRestartComponents.stopFXComponent();
        ComponentShutdownAndRestartComponentsTests1.stopLatch.await();
        perspectives = workbench.getPerspectives();
        assertNotNull(perspectives);
        assertFalse(perspectives.isEmpty());

        for(IPerspective<EventHandler<Event>, Event, Object> p:perspectives) {

            assertTrue(p.getContext().isActive());
            List<ISubComponent<EventHandler<Event>, Event, Object>> components = p.getSubcomponents();

            Thread.sleep(100);
            assertTrue(components.isEmpty());

        }
    }


    @Test
    /**
     * restartComponentTest
     */
    public void test3() throws InterruptedException {
        // Component is shut down
        ApplicationShutdownAndRestartComponentsTest launcher = ApplicationShutdownAndRestartComponentsTest.instance[0];
        AFXWorkbench workbench = launcher.getWorkbench();
        assertNotNull(workbench);
        List<IPerspective<EventHandler<Event>, Event, Object>> perspectives = workbench.getPerspectives();
        assertNotNull(perspectives);
        assertFalse(perspectives.isEmpty());
        for(IPerspective<EventHandler<Event>, Event, Object> p:perspectives) {

            assertTrue(p.getContext().isActive());
            List<ISubComponent<EventHandler<Event>, Event, Object>> components = p.getSubcomponents();

           // Thread.sleep(1000);
            assertTrue(components.isEmpty());

        }
        int i=100;
        while(i>1){
            testStartComponent();
            testStopComponent();
            i--;
        }

    }


    private void testStartComponent() throws InterruptedException {
        ApplicationShutdownAndRestartComponentsTest launcher = ApplicationShutdownAndRestartComponentsTest.instance[0];
        AFXWorkbench workbench = launcher.getWorkbench();
        assertNotNull(workbench);
        List<IPerspective<EventHandler<Event>, Event, Object>> perspectives = workbench.getPerspectives();
        assertNotNull(perspectives);
        assertFalse(perspectives.isEmpty());
        ComponentShutdownAndRestartComponentsTests1.startLatch = new CountDownLatch(1);

        PerspectiveShutdownAndRestartComponents.startComponent();
        ComponentShutdownAndRestartComponentsTests1.startLatch.await();
        Thread.sleep(100);
        for(IPerspective<EventHandler<Event>, Event, Object> p:perspectives) {

            assertTrue(p.getContext().isActive());
            List<ISubComponent<EventHandler<Event>, Event, Object>> components = p.getSubcomponents();
            assertFalse(components.isEmpty());

            components.forEach(c-> assertTrue(c.getContext().isActive()));
        }
    }
}