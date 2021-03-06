package ch.benediktkoeppel.code.droidplane;

import java.util.ArrayList;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;

public class HorizontalMindmapView extends HorizontalScrollView implements OnTouchListener, OnItemClickListener/*, OnItemLongClickListener*/ {
	
	/**
	 * HorizontalScrollView can only have one view, so we need to add a
	 * LinearLayout underneath it, and then stuff all NodeColumns into this
	 * linearLayout.
	 */
	private LinearLayout linearLayout;
	
	/**
	 * nodeColumns holds the list of columns that are displayed in this
	 * HorizontalScrollView.
	 */
	private ArrayList<NodeColumn> nodeColumns;
	
	/**
	 * Gesture detector
	 */
	private GestureDetector gestureDetector;

	private HorizontalMindmapViewGestureDetector horizontalMindmapViewGestureDetector;
	
	/**
	 * constants to determine the minimum swipe distance and speed
	 */
	private static final int SWIPE_THRESHOLD_VELOCITY = 300;
	
	/**
	 * Setting up a HorizontalMindmapView. We initialize the nodeColumns, define
	 * the layout parameters for the HorizontalScrollView and create the
	 * LinearLayout view inside the HorizontalScrollView.
	 * @param context the Application Context
	 */
	public HorizontalMindmapView(Context context) {
		super(context);
		
		// list where all columns are stored
		nodeColumns = new ArrayList<NodeColumn>();
		
		// set the layout for the HorizontalScrollView itself
		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		
		// create the layout parameters for a new LinearLayout
    	int height = LayoutParams.MATCH_PARENT;
    	int width = LayoutParams.MATCH_PARENT;
		ViewGroup.LayoutParams linearLayoutParams = new ViewGroup.LayoutParams(width, height);
		
		// create a LinearLayout in this HorizontalScrollView. All NodeColumns will go into that LinearLayout.
		linearLayout = new LinearLayout(context);
    	linearLayout.setLayoutParams(linearLayoutParams);
    	this.addView(linearLayout);
    	
    	// add a new gesture controller
    	horizontalMindmapViewGestureDetector = new HorizontalMindmapViewGestureDetector();
    	gestureDetector = new GestureDetector(getContext(), horizontalMindmapViewGestureDetector);
    	    	
    	// register HorizontalMindmapView to receive all touch events on itself
    	setOnTouchListener(this);
	}
	
	/**
	 * Add a new NodeColumn to the HorizontalMindmapView
	 * @param nodeColumn the NodeColumn to add to the HorizontalMindmapView
	 */
	public void addColumn(NodeColumn nodeColumn) {
		
		// add the column to the layout
		nodeColumns.add(nodeColumn);
		linearLayout.addView(nodeColumn, linearLayout.getChildCount());
		Log.d(MainApplication.TAG, "linearLayout now has " + linearLayout.getChildCount() + " items");
		
		// register as onItemClickListener and onItemLongClickListener
		nodeColumn.setOnItemClickListener(this);
	}
	
	/**
	 * GUI Helper to scroll the HorizontalMindmapView all the way to the right.
	 * Should be called after adding a NodeColumn.
	 * @return true if the key event is consumed by this method, false otherwise
	 */
	public void scrollToRight() {
		
		// a runnable that knows "this"
		final class HorizontalMindmapViewRunnable implements Runnable {
			HorizontalMindmapView horizontalMindmapView;
			
			public HorizontalMindmapViewRunnable(HorizontalMindmapView horizontalMindmapView) {
				this.horizontalMindmapView = horizontalMindmapView;
			}

			@Override
			public void run() { 
				horizontalMindmapView.fullScroll(FOCUS_RIGHT);
			}
		}
		
		new Handler().postDelayed(new HorizontalMindmapViewRunnable(this), 100L);
	}
	
	/**
	 * Removes all columns from this HorizontalMindmapView
	 */
	public void removeAllColumns() {
		nodeColumns.clear();
		linearLayout.removeAllViews();
	}

	/**
	 * Adjusts the width of all columns in the HorizontalMindmapView
	 * @param columnWidth the width of each column
	 */
	public void resizeAllColumns() {
		for (NodeColumn nodeColumn : nodeColumns) {
			nodeColumn.resizeColumnWidth();
		}
	}
	
	/**
	 * Removes the rightmost column and returns true. If there was no column to
	 * remove, returns false. It never removes the last column, i.e. it never
	 * removes the root node of the mind map.
	 * @return True if a column was removed, false if no column was removed.
	 */
	public boolean removeRightmostColumn() {
		
		// only remove a column if we have at least 2 columns. If there is only
		// one column, it will not be removed.
		if ( nodeColumns.size() >= 2 ) {
			
			// the column to remove
			NodeColumn rightmostColumn = nodeColumns.get(nodeColumns.size()-1);
			
			// remove it from the linear layout
			linearLayout.removeView(rightmostColumn);
			
			// remove it from the nodeColumns list
			nodeColumns.remove(nodeColumns.size()-1);
			
			// then deselect all nodes on the now newly rightmost column and let the column redraw
			nodeColumns.get(nodeColumns.size()-1).deselectAllNodes();
			
			// a column was removed, so we return true
			return true;
		}
		
		// no column was removed, so we return false
		else {
			return false;
		}
	}

	/**
	 * Returns the number of columns in the HorizontalMindmapView.
	 * @return
	 */
	public int getNumberOfColumns() {
		return nodeColumns.size();
	}

	/**
	 * Returns the title of the parent node of the rightmost column. This is the
	 * same as the node name of the selected node from the 2nd-rightmost column.
	 * So this is the last node that the user has clicked.
	 * If the rightmost column has no parent, an empty string is returned.
	 * 
	 * @return Title of the right most parent node or an empty string.
	 */
	public String getTitleOfRightmostParent() {
		
		if ( !nodeColumns.isEmpty() ) {
			
			MindmapNode parent = nodeColumns.get(nodeColumns.size()-1).getParentNode();
			return parent.text;
			
		}
		
		// there were no columns
		else {
			Log.d(MainApplication.TAG, "getTitleOfRightmostParent returned \"\" because nodeColumns is empty");
			return "";
		}
	}
	
	/**
	 * Remove all columns at the right of the specified column. 
	 * @param nodeColumn
	 */
	public void removeAllColumnsRightOf(NodeColumn nodeColumn) {
		
		// we go from right to left, from the end of nodeColumns back to one
		// element after nodeColumn
		//		
		// nodeColumns = [ col1, col2, col3, col4, col5 ];
		// removeAllColumnsRightOf(col2) will do:
		//     nodeColumns.size()-1 => 4
		//     nodeColumns.lastIndexOf(col2)+1 => 2
		//
		// for i in (4, 3, 2): remove rightmost column
		//     i = 4: remove col5
		//     i = 3: remove col4
		//     i = 2: remove col3
		//
		// so at the end, we have
		// nodeColumns = [ col1, col2 ];
		for (int i = nodeColumns.size()-1; i >= nodeColumns.lastIndexOf(nodeColumn)+1; i--) {
			
			// remove this column
			removeRightmostColumn();
		}
	}

	/**
	 * navigates to the top of the Mindmap
	 */
	public void top() {

		// remove all ListView layouts in linearLayout parent_list_view
		removeAllColumns();

		// go down into the root node
		down(MainApplication.getInstance().mindmap.getRootNode());
	}

	/**
	 * navigates back up one level in the Mindmap, if possible (otherwise does
	 * nothing)
	 */
	public void up() {
		up(false);
	}

	/**
	 * navigates back up one level in the Mindmap. If we already display the
	 * root node, the application will finish
	 */
	public void upOrClose() {
		up(true);
	}

	/**
	 * navigates back up one level in the Mindmap, if possible. If force is
	 * true, the application closes if we can't go further up
	 * 
	 * @param force
	 */
	public void up(boolean force) {

		boolean wasColumnRemoved = removeRightmostColumn();

		// close the application if no column was removed, and the force switch
		// was on
		if (!wasColumnRemoved && force) {
			MainApplication.getMainActivityInstance().finish();
		}

		// enable the up navigation with the Home (app) button (top left corner)
		enableHomeButtonIfEnoughColumns();

		// get the title of the parent of the rightmost column (i.e. the
		// selected node in the 2nd-rightmost column)
		setApplicationTitle();

	}

	/**
	 * open up Node node, and display all its child nodes
	 * 
	 * @param node
	 */
	public void down(MindmapNode node) {

		// add a new column for this node and add it to the
		// HorizontalMindmapView
		NodeColumn nodeColumn = new NodeColumn(getContext(), node);
		addColumn(nodeColumn);

		// then scroll all the way to the right
		scrollToRight();

		// enable the up navigation with the Home (app) button (top left corner)
		enableHomeButtonIfEnoughColumns();

		// get the title of the parent of the rightmost column (i.e. the
		// selected node in the 2nd-rightmost column)
		setApplicationTitle();

	}

	/**
	 * Sets the application title to the name of the parent node of the
	 * rightmost column, which is the most recently clicked node.
	 */
	void setApplicationTitle() {
		// get the title of the parent of the rightmost column (i.e. the
		// selected node in the 2nd-rightmost column)
		// set the application title to this nodeTitle. If the nodeTitle is
		// empty, we set the default Application title
		String nodeTitle = getTitleOfRightmostParent();
		Log.d(MainApplication.TAG, "nodeTitle = " + nodeTitle);
		if (nodeTitle.equals("")) {
			Log.d(MainApplication.TAG, "Setting application title to default string: " + getResources().getString(R.string.app_name));
			MainApplication.getMainActivityInstance().setTitle(R.string.app_name);
		} else {
			Log.d(MainApplication.TAG,"Setting application title to node name: " + nodeTitle);
			MainApplication.getMainActivityInstance().setTitle(nodeTitle);
		}
	}

	/**
	 * Enables the Home button in the application if we have enough columns,
	 * i.e. if "Up" will remove a column.
	 */
	void enableHomeButtonIfEnoughColumns() {
		// if we only have one column (i.e. this is the root node), then we
		// disable the home button
		int numberOfColumns = getNumberOfColumns();
		if (numberOfColumns >= 2) {
			MainApplication.getMainActivityInstance().enableHomeButton();
		} else {
			MainApplication.getMainActivityInstance().disableHomeButton();
		}
	}
	

	
	/* (non-Javadoc)
	 * Handler when one of the ListItem's item is clicked
	 * Find the node which was clicked, and redraw the screen with this node as new parent
	 * if the clicked node has no child, then we stop here
	 * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		
		// the clicked column
		// parent is the ListView in which the user clicked. Because
		// NodeColumn does not extend ListView (it only wraps a ListView), we
		// have to find out which NodeColumn it was. We can do so because
		// NodeColumn.getNodeColumnFromListView uses a static HashMap to do the
		// translation.
		NodeColumn clickedNodeColumn = NodeColumn.getNodeColumnFromListView((ListView)parent);
		
		// remove all columns right of the column which was clicked
		removeAllColumnsRightOf(clickedNodeColumn);
		
		// then get the clicked node
		MindmapNode clickedNode = clickedNodeColumn.getNodeAtPosition(position);
		
		// if the clicked node has child nodes, we set it to selected and drill down
		if ( clickedNode.getNumChildMindmapNodes() > 0 ) {
			
			// give it a special color
			clickedNodeColumn.setItemColor(position);
			
			// and drill down
			down(clickedNode);
		}

        // if the clicked node has a link (and is a leaf), open the link
        else if (clickedNode.link != null) {
            clickedNode.openLink();
        }

        // otherwise (no children) then we just update the application title to the new parent node
		else {
			setApplicationTitle();
		}
	}
	
	
	/*
	 * (non-Javadoc)
	 * Will be called whenever the HorizontalScrollView is
	 * touched. We have to capture the move left and right events here, and snap
	 * to the appropriate column borders.
	 * 
	 * @see android.view.View.OnTouchListener#onTouch(android.view.View,
	 * android.view.MotionEvent)
	 */
	@Override
	public boolean onTouch(View view, MotionEvent event) {
		
		// first, we let the gestureDetector examine the event. It will process
		// the event if it was a gesture, i.e. if it was fast enough to trigger
		// a Fling. If it handled the event, we don't process it further.
		// This gesture can be triggered if the user moves the finger fast
		// enough. He does not necessarily have to move so far that the next
		// column is mostly visible.
		if ( gestureDetector.onTouchEvent(event) ) {
			Log.d(MainApplication.TAG, "Touch event was processed by HorizontalMindmapView (gesture)");
			return true;
		}
		
		// If it was not a gesture (i.e. the user moved his finger too slow), we
		// simply snap to the next closest column border.
		else if ( event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL ) {
			
			// now we need to find out where the HorizontalMindmapView is horizontally scrolled
			int scrollX = getScrollX();
			Log.d(MainApplication.TAG, "HorizontalMindmapView is scrolled horizontally to " + scrollX);
		
			// get the leftmost column that is still (partially) visible
			NodeColumn leftmostVisibleColumn = getLeftmostVisibleColumn();
			
			// get the number of visible pixels of this column
			int numVisiblePixelsOnColumn = getVisiblePixelOfLeftmostColumn();
			
			// if we couldn't find a column, we could not process this event. I'm not sure how this might ever happen
			if ( leftmostVisibleColumn == null ) {
				Log.e(MainApplication.TAG, "No leftmost visible column was detected. Not sure how this could happen!");
				return false;
			}
			
			// and then determine if the leftmost visible column shows more than 50% of its full width
			// if it shows more than 50%, then we scroll to the left, so that we can see it fully
			if ( numVisiblePixelsOnColumn < leftmostVisibleColumn.getWidth()/2 ) {
				Log.d(MainApplication.TAG, "Scrolling to the left, so that we can see the column fully");
				smoothScrollTo(scrollX + numVisiblePixelsOnColumn, 0);
			}
			
			// if it shows less than 50%, then we scroll to the right, so that is not visible anymore 
			else {
				Log.d(MainApplication.TAG, "Scrolling to the right, so that the column is not visible anymore");
				smoothScrollTo(scrollX + numVisiblePixelsOnColumn - leftmostVisibleColumn.getWidth(), 0);
			}
			
			// we have processed this event
			Log.d(MainApplication.TAG, "Touch event was processed by HorizontalMindmapView (no gesture)");
			return true;
			
		}
		
		// if we did not process the event ourself we let the caller know
		else {
			Log.d(MainApplication.TAG, "Touch event was not processed by HorizontalMindmapView");
			return false;
		}
	}
	
	/**
	 * Get the column at the left edge of the screen.
	 * @return NodeColumn
	 */
	private NodeColumn getLeftmostVisibleColumn() {

		// how much we are horizontally scrolled
		int scrollX = getScrollX();
		
		// how many columns fit into less than scrollX space? as soon as
		// sumColumnWdiths > scrollX, we have just added the first visible
		// column at the left.
		int sumColumnWidths = 0;
		NodeColumn leftmostVisibleColumn = null;
		for (int i = 0; i < nodeColumns.size(); i++) {
			sumColumnWidths += nodeColumns.get(i).getWidth();
			
			// if the sum of all columns so far exceeds scrollX, the current NodeColumn is (at least a little bit) visible
			if (sumColumnWidths >= scrollX) {
				leftmostVisibleColumn = nodeColumns.get(i);
				break;
			}
		}
		
		return leftmostVisibleColumn;
	}
	
	/**
	 * Get the number of pixels that are visible on the leftmost column.
	 * @return
	 */
	private int getVisiblePixelOfLeftmostColumn() {
		
		// how much we are horizontally scrolled
		int scrollX = getScrollX();
		
		// how many columns fit into less than scrollX space? as soon as
		// sumColumnWdiths > scrollX, we have just added the first visible
		// column at the left.
		int sumColumnWidths = 0;
		int numVisiblePixelsOnColumn = 0;
		for (int i = 0; i < nodeColumns.size(); i++) {
			sumColumnWidths += nodeColumns.get(i).getWidth();
			
			// if the sum of all columns so far exceeds scrollX, the current NodeColumn is (at least a little bit) visible
			if (sumColumnWidths >= scrollX) {
				// how many pixels are visible of this column?
				numVisiblePixelsOnColumn = sumColumnWidths - scrollX;
				break;
			}
		}
		
		return numVisiblePixelsOnColumn;
	}
	
	/**
	 * The HorizontalMindmapViewGestureDetector should detect the onFling event.
	 * However, it never receives the onDown event, so when it gets the onFling
	 * the event1 is empty, and we can't detect the fling properly.
	 */
	class HorizontalMindmapViewGestureDetector extends SimpleOnGestureListener {
		
		@Override
	    public boolean onDown(MotionEvent e) {
	        return true;
	    }

		/*
		 * (non-Javadoc) onFling is called whenever a Fling (a fast swipe) event
		 * is detected. However, for some reason, our onDown method is never
		 * called, and the onFling method never gets a valid event1 (it's always
		 * null). So instead of relying on event1 and event2 (and determine the
		 * distance the finger moved), we only consider the velocity of the
		 * fling. This is not as accurate as it could be, but it works.
		 * 
		 * @see
		 * android.view.GestureDetector.SimpleOnGestureListener#onFling(android
		 * .view.MotionEvent, android.view.MotionEvent, float, float)
		 */
		@Override
		public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
			
			try {
				
				// how much we are horizontally scrolled
				int scrollX = getScrollX();
				Log.d(MainApplication.TAG, "Velocity = " + velocityX);
				
				// get the leftmost column that is still (partially) visible
				NodeColumn leftmostVisibleColumn = getLeftmostVisibleColumn();
				
				// get the number of visible pixels of this column
				int numVisiblePixelsOnColumn = getVisiblePixelOfLeftmostColumn();
				
				// if we have moved at least the SWIPE_MIN_DISTANCE to the right and at faster than SWIPE_THRESHOLD_VELOCITY
				if (velocityX < 0 && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					
					// scroll to the target column
					smoothScrollTo(scrollX + numVisiblePixelsOnColumn, 0);
					
					Log.d(MainApplication.TAG, "processed the Fling to Right gesture");
					return true;
				}
				
				// the same as above but from to the left
				else if ( velocityX > 0 && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
	
					// scroll to the target column
					// scrolls in the wrong direction
					smoothScrollTo(scrollX + numVisiblePixelsOnColumn - leftmostVisibleColumn.getWidth(), 0);
					
					Log.d(MainApplication.TAG, "processed the Fling to Left gesture");
					return true;
				}
				
				// we did not process this gesture
				else {
					Log.d(MainApplication.TAG, "Fling was no real fling");
					return false;
				}
				
			} catch (Exception e) {
				Log.d(MainApplication.TAG, "A whole lot of stuff could have gone wrong here");
				e.printStackTrace();
				return false;
			}
		}
	}
}