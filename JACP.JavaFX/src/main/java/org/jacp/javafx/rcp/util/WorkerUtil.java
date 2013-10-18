package org.jacp.javafx.rcp.util;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import org.jacp.api.action.IAction;
import org.jacp.api.action.IActionListener;
import org.jacp.api.annotations.lifecycle.PreDestroy;
import org.jacp.api.component.ISubComponent;
import org.jacp.api.component.IUIComponent;
import org.jacp.api.util.UIType;
import org.jacp.javafx.rcp.action.FXAction;
import org.jacp.javafx.rcp.component.AComponent;
import org.jacp.javafx.rcp.component.AFXComponent;
import org.jacp.javafx.rcp.context.JACPContextImpl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created with IntelliJ IDEA.
 * User: Andy Moncsek
 * Date: 11.10.13
 * Time: 14:22
 * This util class contains methods needed in all types  of workers
 */
public class WorkerUtil {

    /**
     * invokes a runnable on application thread and waits until execution is
     * finished
     *
     * @param runnable, a runnable which will be invoked and wait until execution is finished
     * @throws InterruptedException,java.util.concurrent.ExecutionException
     *
     */
    public static void invokeOnFXThreadAndWait(final Runnable runnable)
            throws InterruptedException, ExecutionException {
        final Lock lock = new ReentrantLock();
        final Condition condition = lock.newCondition();
        final AtomicBoolean conditionReady = new AtomicBoolean(false);
        final ThrowableWrapper throwableWrapper = new ThrowableWrapper();
        lock.lock();
        try {
            Platform.runLater(() -> {
                lock.lock();
                try {
                    // prevent execution when application is closed
                    if (ShutdownThreadsHandler.APPLICATION_RUNNING.get())
                        runnable.run();
                } catch (Throwable t) {
                    throwableWrapper.t = t;
                } finally {
                    conditionReady.set(true);
                    condition.signal();
                    lock.unlock();
                }

            });
            // wait until execution is finished and check if application is
            // still running to prevent wait
            while (!conditionReady.get()
                    && ShutdownThreadsHandler.APPLICATION_RUNNING.get())
                condition.await(ShutdownThreadsHandler.WAIT,
                        TimeUnit.MILLISECONDS);
            if (throwableWrapper.t != null) {
                throw new ExecutionException(throwableWrapper.t);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * checks if component was deactivated, if so run OnTeardown annotations.
     *
     * @param component, the component
     */
    public static void runCallbackOnTeardownMethods(
            final ISubComponent<EventHandler<Event>, Event, Object> component) {

        // turn off component
        if (!component.getContext().isActive()) {
            FXUtil.setPrivateMemberValue(AComponent.class, component,
                    FXUtil.ACOMPONENT_STARTED, false);
            // run teardown
            FXUtil.invokeHandleMethodsByAnnotation(PreDestroy.class, component.getComponentHandle());
        }
    }

    /**
     * find valid target and add type specific new component. Handles Container,
     * ScrollPanes, Menus and Bar Entries from user
     *
     * @param validContainer, a valid container where components root will be added
     * @param component,      the component
     */
    public static void addComponentByType(
            final Node validContainer,
            final IUIComponent<Node, EventHandler<Event>, Event, Object> component) {
        handleAdd(validContainer, component.getRoot());
        handleViewState(validContainer, true);

    }

    /**
     * enables component an add to container
     *
     * @param validContainer , a valid container where components root will be added
     * @param IUIComponent   , the component
     */
    private static void handleAdd(final Node validContainer, final Node IUIComponent) {
        if (validContainer != null && IUIComponent != null) {
            handleViewState(IUIComponent, true);
            final ObservableList<Node> children = FXUtil
                    .getChildren(validContainer);
            children.add(IUIComponent);
        }

    }

    /**
     * set visibility and enable/disable
     *
     * @param IUIComponent, a Node where to set the state
     * @param state,        the boolean value of the state
     */
    public static void handleViewState(final Node IUIComponent,
                                       final boolean state) {
        IUIComponent.setVisible(state);
        IUIComponent.setDisable(!state);
        IUIComponent.setManaged(state);
    }

    /**
     * delegate components handle return value to specified target
     *
     * @param comp,     the component
     * @param targetId, the message target id
     * @param value,    the message value
     * @param action,   the action
     */
    public static void delegateReturnValue(
            final ISubComponent<EventHandler<Event>, Event, Object> comp,
            final String targetId, final Object value,
            final IAction<Event, Object> action) {
        if (value != null && targetId != null
                && !action.isMessage("init")) {
            final IActionListener<EventHandler<Event>, Event, Object> listener = comp.getContext()
                    .getActionListener(null);
            listener.notifyComponents(new FXAction(comp.getContext().getId(), targetId,
                    value, null));
        }
    }

    /**
     * Executes post handle method in application main thread. The result value
     * of handle method (from worker thread) is Input for the postHandle Method.
     * The return value or the handleReturnValue are the root node of this
     * component.
     *
     * @param component, a component
     * @param action,    the current action
     */
    public static void executeComponentViewPostHandle(final Node handleReturnValue,
                                                            final AFXComponent component, final IAction<Event, Object> action) throws Exception {

        Node potsHandleReturnValue = component.getComponentViewHandle().postHandle(handleReturnValue,
                action);
        if (potsHandleReturnValue == null) {
            potsHandleReturnValue = handleReturnValue;
        } else if (component.getType().equals(UIType.DECLARATIVE)) {
            throw new UnsupportedOperationException(
                    "declarative components should not have a return value in postHandle method, otherwise you would overwrite the FXML root node.");
        }
        if (potsHandleReturnValue != null
                && component.getType().equals(UIType.PROGRAMMATIC)) {
            potsHandleReturnValue.setVisible(true);
            component.setRoot(potsHandleReturnValue);
        }
    }

    /**
     * Move component to new target in perspective.
     *
     * @param delegateQueue, the component delegate queue
     * @param component,     the component
     */
    public static void changeComponentTarget(
            final BlockingQueue<ISubComponent<EventHandler<Event>, Event, Object>> delegateQueue,
            final ISubComponent<EventHandler<Event>, Event, Object> component) {
        final String targetId = JACPContextImpl.class.cast(component.getContext()).getExecutionTarget();
        final String parentIdOld = component.getParentId();
        final String parentId = FXUtil.getTargetParentId(targetId);
        if (!parentIdOld.equals(parentId)) {
            // delegate to perspective observer
            delegateQueue.add(component);

        }
    }

    /**
     * Runs the handle method of a componentView.
     *
     * @param component, the component
     * @param action,    the current action
     * @return a returned node from component execution
     */
    public static Node prepareAndRunHandleMethod(
            final IUIComponent<Node, EventHandler<Event>, Event, Object> component,
            final IAction<Event, Object> action) throws Exception {
        return component.getComponentViewHandle().handle(action);

    }

}
