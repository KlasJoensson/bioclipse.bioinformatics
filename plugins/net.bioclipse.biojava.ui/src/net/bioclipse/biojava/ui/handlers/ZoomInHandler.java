package net.bioclipse.biojava.ui.handlers;

import net.bioclipse.biojava.ui.editors.SequenceEditor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;


public class ZoomInHandler extends AbstractHandler implements IHandler {

    public Object execute( ExecutionEvent event ) throws ExecutionException {

        IEditorPart editor = HandlerUtil.getActiveEditor( event );

        if (!(editor instanceof SequenceEditor))
            return null;

        ((SequenceEditor) editor).zoomIn();
        
        return null;
    }

}
