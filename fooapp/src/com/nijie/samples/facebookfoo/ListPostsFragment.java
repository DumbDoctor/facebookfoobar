package com.nijie.samples.facebookfoo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.facebook.Request;
import com.facebook.Session;
import com.facebook.model.GraphPlace;
import com.facebook.widget.GraphObjectPagingLoader;
import com.facebook.widget.PickerFragment;
import com.facebook.widget.SimpleGraphObjectCursor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ListPostsFragment extends PickerFragment<GraphPlace>{

	private static final String ID = "id";
    private static final String NAME = "name";
    private static final String FROM = "from";
    private static final String MESSAGE = "message";
    private static final String UPDATED_TIME = "updated_time";
    private static final String IS_PUBLISHED = "is_published";
	private String page_id = null;

    /**
     * Default constructor. Creates a Fragment with all default properties.
     */
    public ListPostsFragment() {
        this(null);
    }

    /**
     * Constructor.
     * @param args  a Bundle that optionally contains one or more values containing additional
     *              configuration information for the Fragment.
     */
    @SuppressLint("ValidFragment")
    public ListPostsFragment(Bundle args) {
        super(GraphPlace.class, R.layout.listpost_fragment, args);
        setPostPickerSettingsFromBundle(args);
    }


    private void setPostPickerSettingsFromBundle(Bundle inState) {

    }

	public void setTargetPageID(String page_id){
		this.page_id = page_id;
	}
		


    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);
        TypedArray a = activity.obtainStyledAttributes(attrs, R.styleable.listpost_fragment);

        //setRadiusInMeters(a.getInt(R.styleable.com_facebook_place_picker_fragment_radius_in_meters, radiusInMeters));
        //setResultsLimit(a.getInt(R.styleable.com_facebook_place_picker_fragment_results_limit, resultsLimit));
        //if (a.hasValue(R.styleable.com_facebook_place_picker_fragment_results_limit)) {
         //   setSearchText(a.getString(R.styleable.com_facebook_place_picker_fragment_search_text));
        //}
        //showSearchBox = a.getBoolean(R.styleable.com_facebook_place_picker_fragment_show_search_box, showSearchBox);

        a.recycle();
    }
	
    @Override
    public void setupViews(ViewGroup view) {
        
    }
	
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }	

    public void saveSettingsToBundle(Bundle outState) {
        super.saveSettingsToBundle(outState);
    }

    private Request createRequest(Session session) {
		
		Request request = Request.newMyPageRequest(session, page_id+"/promotable_posts", null);


        Set<String> fields = new HashSet<String>();
        String[] requiredFields = new String[]{
                ID,
                NAME,
                FROM,
                MESSAGE,
                UPDATED_TIME,
                IS_PUBLISHED
                
                
        };
        fields.addAll(Arrays.asList(requiredFields));

        //String pictureField = adapter.getPictureFieldSpecifier();
        //if (pictureField != null) {
        //    fields.add(pictureField);
        //}

        Bundle parameters = request.getParameters();
        parameters.putString("fields", TextUtils.join(",", fields));
        request.setParameters(parameters);

		

        return request;
    }

    public List<GraphPlace> getSelection() {
        return getSelectedGraphObjects();
    }

	@Override
    public Request getRequestForLoadData(Session session) {
		return createRequest(session);
		//return null;
	}

	@Override
    public String getDefaultTitleText() {
	   return getString(R.string.listpost_title);
	}

	@Override
    public void logAppEvents(boolean doneButtonClicked) {
	   //AppEventsLogger logger = AppEventsLogger.newLogger(this.getActivity(), getSession());
	   //Bundle parameters = new Bundle();

	   // If Done was clicked, we know this completed successfully. If not, we don't know (caller might have
	   // dismissed us in response to selection changing, or user might have hit back button). Either way
	   // we'll log the number of selections.
	  // String outcome = doneButtonClicked ? AnalyticsEvents.PARAMETER_DIALOG_OUTCOME_VALUE_COMPLETED :
	//		   AnalyticsEvents.PARAMETER_DIALOG_OUTCOME_VALUE_UNKNOWN;
	 //  parameters.putString(AnalyticsEvents.PARAMETER_DIALOG_OUTCOME, outcome);
	 //  parameters.putInt("num_places_picked", (getSelection() != null) ? 1 : 0);

	 //  logger.logSdkEvent(AnalyticsEvents.EVENT_PLACE_PICKER_USAGE, null, parameters);
	}


	@Override
    public PickerFragmentAdapter<GraphPlace> createAdapter() {
		PickerFragmentAdapter<GraphPlace> adapter = new PickerFragmentAdapter<GraphPlace>(
				this.getActivity()) {
			@Override
			protected CharSequence getSubTitleOfGraphObject(GraphPlace graphObject) {
				//String category = graphObject.getCategory();
				//Integer wereHereCount = (Integer) graphObject.getProperty(WERE_HERE_COUNT);
				String result = null;
				//String from =  (String) graphObject.getProperty(FROM);
				String message =  (String) graphObject.getProperty(MESSAGE);
				String time =  (String) graphObject.getProperty(UPDATED_TIME);
				boolean is_published =  (boolean) graphObject.getProperty(IS_PUBLISHED);

				
				result = System.getProperty ("line.separator")+"message: "+message+System.getProperty ("line.separator")+"at: "+time+System.getProperty ("line.separator")+"published?: "+is_published;

				return result;
			}

			@Override
			protected int getGraphObjectRowLayoutId(GraphPlace graphObject) {
				return R.layout.listpost_fragment_list_row;
			}

			@Override
			protected int getDefaultPicture() {
				return R.drawable.com_facebook_place_default_icon;
			}

		};
		adapter.setShowCheckbox(false);
		adapter.setShowPicture(getShowPictures());
		return adapter;
	}

	@Override
    public LoadingStrategy createLoadingStrategy() {
		return new ImmediateLoadingStrategy();
	}

	@Override
    public SelectionStrategy createSelectionStrategy() {
		return new SingleSelectionStrategy();
	}

	 private class ImmediateLoadingStrategy extends LoadingStrategy {
		@Override
		public void onLoadFinished(GraphObjectPagingLoader<GraphPlace> loader,
				SimpleGraphObjectCursor<GraphPlace> data) {
			super.onLoadFinished(loader, data);
		
			// We could be called in this state if we are clearing data or if we are being re-attached
			// in the middle of a query.
			if (data == null || loader.isLoading()) {
				return;
			}
		
			if (data.areMoreObjectsAvailable()) {
				// We got results, but more are available.
				followNextLink();
			} else {
				// We finished loading results.
				hideActivityCircle();
		
				// If this was from the cache, schedule a delayed refresh query (unless we got no results
				// at all, in which case refresh immediately.
				if (data.isFromCache()) {
					loader.refreshOriginalRequest(data.getCount() == 0 ? CACHED_RESULT_REFRESH_DELAY : 0);
				}
			}
		}


        @Override
        public boolean canSkipRoundTripIfCached() {
            //return friendPickerType.isCacheable();
            return false;
        }

        public void followNextLink() {
            // This may look redundant, but this causes the circle to be alpha-dimmed if we have results.
            displayActivityCircle();

            loader.followNextLink();
        }
    }


}


