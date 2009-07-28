package net.bioclipse.biojava.ui.views.outline;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.bioclipse.biojava.ui.editors.Aligner;
import net.bioclipse.biojava.ui.editors.SequenceEditor;

import org.biojava.bio.BioException;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.seq.SequenceIterator;
import org.biojavax.bio.seq.RichSequence.IOTools;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

public class SequenceOutlinePage extends Page
                                 implements IContentOutlinePage,
                                            ISelectionListener, IAdaptable {

    private int squareSize = 8;
    private final static int MINIMUM_SQUARE_SIZE_FOR_TEXT_IN_PIXELS = 8;
    private int canvasWidthInSquares, canvasHeightInSquares;

    private Canvas sequenceCanvas;
    
    //          seqname, sequence
    private Map<String,  String> sequences;
    private int consensusRow;

    public SequenceOutlinePage(IEditorInput input, SequenceEditor editor) {
        super();

        setInput(input);
    }
    
    public void setInput( IEditorInput input ) {
        sequences = new LinkedHashMap<String, String>();

        // Turn the editor input into an IFile.
        IFile file = (IFile) input.getAdapter( IFile.class );
        if (file == null)
            return;

        SequenceIterator iter;
        try {
            // Create a BufferedInputStream for our IFile.
            BufferedReader br
                = new BufferedReader(new InputStreamReader(file.getContents()));

            // Create an iterator from the BufferedInputStream.
            // We have to generalize this from just proteins to anything.
            // The 'null' indicates that we don't care about which
            // namespace the sequence ends up getting.
            iter = IOTools.readFastaProtein( br, null );
        } catch ( CoreException ce ) {
            // File not found. TODO: This should be logged.
            ce.printStackTrace();
            return;
        }

        try {
            // Add the sequences one by one to the Map. Do minor cosmetics
            // on the name by removing everything up to and including to
            // the last '|', if any.
            while ( iter.hasNext() ) {
                Sequence s = iter.nextSequence();
                String name = s.getName().replaceFirst( ".*\\|", "" );
                sequences.put( name, s.seqString() );
            }
        }
        catch ( BioException e ) {
            // There was a parsing error. TODO: This should be logged.
            e.printStackTrace();
        }

        // We only show a consensus sequence if there is more than one
        // sequence already.
        consensusRow  = sequences.size();
        if (consensusRow > 1) {
            sequences.put(
                "Consensus",
                consensusSequence( sequences.values() )
            );
        }

        canvasHeightInSquares = sequences.size();
        canvasWidthInSquares = maxLength( sequences.values() );
    }

    private static String consensusSequence( final Collection<String>
                                             sequences ) {

        final StringBuilder consensus = new StringBuilder();
        for ( int i = 0, n = maxLength(sequences); i < n; ++i )
            consensus.append( consensusChar(sequences, i) );

        return consensus.toString();
    }

    private static int maxLength( final Collection<String> strings ) {

        int maxLength = 0;
        for ( String s : strings )
            if ( maxLength < s.length() )
                maxLength = s.length();

        return maxLength;
    }

    private static char consensusChar( final Collection<String> sequences,
                                       final int index ) {
        
        Map<Character, Boolean> columnChars
            = new HashMap<Character, Boolean>();

        for ( String seq : sequences )
            columnChars.put( seq.length() > index
                               ? seq.charAt(index)
                               : '\0',
                             true );

        return columnChars.size() == 1
               ? columnChars.keySet().iterator().next()
               : Character.forDigit( Math.min(columnChars.size(), 9), 10 );
    }

    public void createControl(Composite parent) {
        
        sequenceCanvas = new Canvas( parent, SWT.H_SCROLL );
        sequenceCanvas.setSize( canvasWidthInSquares*squareSize,canvasHeightInSquares*squareSize );
        ScrollBar sb =sequenceCanvas.getHorizontalBar(); 
        sb.setMaximum( canvasWidthInSquares );
        sb.addSelectionListener( new SelectionListener(){

            public void widgetDefaultSelected( SelectionEvent e ) {
            }

            public void widgetSelected( SelectionEvent e ) {
                sequenceCanvas.redraw();
            }
        });
        

        final char fasta[][] = new char[ sequences.size() ][];

        {
            int i = 0;
            for ( String sequence : sequences.values() )
                fasta[i++] = sequence.toCharArray();
        }

        sequenceCanvas.addPaintListener( new PaintListener() {
            public void paintControl(PaintEvent e) {
                GC gc = e.gc;
                if ( squareSize >= MINIMUM_SQUARE_SIZE_FOR_TEXT_IN_PIXELS ) {
                    gc.setTextAntialias( SWT.ON );
                    gc.setFont( new Font(gc.getDevice(),
                                         "Arial",
                                         (int)(.7 * squareSize),
                                         SWT.NONE) );
                    gc.setForeground( Aligner.textColor );
                }

                int firstVisibleColumn
                        = sequenceCanvas.getHorizontalBar().getSelection(),
                    lastVisibleColumn
                        = sequences.values().iterator().next().length();

                drawSequences(fasta, firstVisibleColumn, lastVisibleColumn, gc);
                drawConsensusSequence(
                    fasta[canvasHeightInSquares-1],
                    firstVisibleColumn, lastVisibleColumn, gc);
            }

            private void drawSequences( final char[][] fasta,
                                        int firstVisibleColumn,
                                        int lastVisibleColumn, GC gc ) {
                int xCoord = 0;
                for ( int column = firstVisibleColumn;
                      column < lastVisibleColumn; ++column ) {

                    for ( int row = 0; row < canvasHeightInSquares-1; ++row ) {
                        
                        char c = fasta[row].length > column
                                 ? fasta[row][column] : ' ';
                        String cc = c + "";

                        gc.setBackground(
                             "HKR".contains( cc ) ? Aligner.basicAAColor
                          :   "DE".contains( cc ) ? Aligner.acidicAAColor
                          : "TQSN".contains( cc ) ? Aligner.polarAAColor
                          :  "FYW".contains( cc ) ? Aligner.nonpolarAAColor
                          :   "GP".contains( cc ) ? Aligner.smallAAColor
                          :    'C' == c           ? Aligner.cysteineColor
                                                  : Aligner.normalAAColor );

                        int yCoord = row * squareSize;

                        gc.fillRectangle(xCoord, yCoord,
                                         squareSize, squareSize);

                        if ( Character.isUpperCase( c )
                             && squareSize
                                  >= MINIMUM_SQUARE_SIZE_FOR_TEXT_IN_PIXELS )
                            gc.drawText( "" + c, xCoord + 4, yCoord + 2 );
                    }
                    xCoord += squareSize;
                }
            }

            private void drawConsensusSequence( final char[] sequence,
                                                int firstVisibleColumn,
                                                int lastVisibleColumn, GC gc ) {

                int yCoord = (canvasHeightInSquares-1) * squareSize;
                int xCoord = 0;
                for ( int column = firstVisibleColumn;
                      column < lastVisibleColumn; ++column ) {

                    char c = sequence.length > column ? sequence[column] : ' ';
                    int consensusDegree = Character.isDigit(c) ? c - '0' : 1;

                    gc.setBackground(
                        Aligner.consensusColors[ consensusDegree-1 ]);

                    gc.fillRectangle(xCoord, yCoord, squareSize, squareSize);

                    if ( Character.isUpperCase( c )
                         && squareSize
                              >= MINIMUM_SQUARE_SIZE_FOR_TEXT_IN_PIXELS )
                        gc.drawText( "" + c, xCoord + 4, yCoord + 2 );

                    xCoord += squareSize;
                }
            }
        });
    }

    public void selectionChanged( IWorkbenchPart part, ISelection selection ) {
    }

    @SuppressWarnings("unchecked")
    public Object getAdapter( Class adapter ) {
        return null;
    }

    @Override
    public Control getControl() {
        return sequenceCanvas;
    }

    @Override
    public void setFocus() {
    }

    public void addSelectionChangedListener(
        ISelectionChangedListener listener) {
    }

    public ISelection getSelection() {
        return null;
    }

    public void removeSelectionChangedListener(
            ISelectionChangedListener listener ) {
    }

    public void setSelection( ISelection selection ) {
    }
}
