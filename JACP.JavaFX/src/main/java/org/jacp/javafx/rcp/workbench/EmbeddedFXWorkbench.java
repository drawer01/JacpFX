package org.jacp.javafx.rcp.workbench;

/**
 * Created with IntelliJ IDEA.
 * User: ady
 * Date: 30.08.13
 * Time: 11:53
 * To change this template use File | Settings | File Templates.
 */
public class EmbeddedFXWorkbench extends AFXWorkbench {

    public EmbeddedFXWorkbench(final FXWorkbench handler){
        this.setComponentHandle(handler);
    }

}
