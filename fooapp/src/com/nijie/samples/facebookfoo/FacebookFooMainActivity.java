/**
 * Copyright 2010-present Facebook.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nijie.samples.facebookfoo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AppEventsLogger;
import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.FacebookRequestError;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphPlace;
import com.facebook.model.GraphUser;
import com.facebook.widget.FacebookDialog;
import com.facebook.widget.FriendPickerFragment;
import com.facebook.widget.LoginButton;
import com.facebook.widget.PickerFragment;
import com.facebook.widget.PlacePickerFragment;
import com.facebook.widget.ProfilePictureView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;




public class FacebookFooMainActivity extends FragmentActivity {

    private static final String PERMISSION = "publish_actions";
	private static final String PAGE_PERMISSION = "manage_pages";
    private static final Location SEATTLE_LOCATION = new Location("") {
        {
            setLatitude(47.6097);
            setLongitude(-122.3331);
        }
    };

    private final String PENDING_ACTION_BUNDLE_KEY = "com.nijie.samples.facebookfoo:PendingAction";

    private Button postRegularPostButton;
    private Button postUnpublishedPostButton;
    private Button listAllPostsButton;
    private Button showStatisticsButton;
    private LoginButton loginButton;
    private ProfilePictureView profilePictureView;
    private TextView greeting;
    private PendingAction pendingAction = PendingAction.NONE;
    private ViewGroup controlsContainer;
    private GraphUser user;
    private GraphPlace place;
    private List<GraphUser> tags;
    private boolean canPresentShareDialog;
    private boolean canPresentShareDialogWithPhotos;

	private Session userInfoSession = null;
	private Session pageManageSession = null;

	private final Context mContext = null;
	private String page_id = null;

    private final HashMap<String, PostsRecord> postsTable = new HashMap<String, PostsRecord>();
	

    private enum PendingAction {
        NONE,
        POST_PHOTO,
        POST_PUBLISHED,
        POST_UNPUBLISHED
    }
    private UiLifecycleHelper uiHelper;

    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };

    private FacebookDialog.Callback dialogCallback = new FacebookDialog.Callback() {
        @Override
        public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data) {
            Log.d("facebookfoo", String.format("Error: %s", error.toString()));
        }

        @Override
        public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data) {
            Log.d("facebookfoo", "Success!");
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.d("facebookfoo ###############", "FacebookFooMainActivity create!");
        super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            String name = savedInstanceState.getString(PENDING_ACTION_BUNDLE_KEY);
            pendingAction = PendingAction.valueOf(name);
        }

        setContentView(R.layout.main);

        loginButton = (LoginButton) findViewById(R.id.login_button);
		loginButton.setPublishPermissions(PAGE_PERMISSION);
        loginButton.setUserInfoChangedCallback(new LoginButton.UserInfoChangedCallback() {
            @Override
            public void onUserInfoFetched(GraphUser user) {
                FacebookFooMainActivity.this.user = user;
				Log.d("facebookfoo ###############", "onUserInfoFetched!");
                updateUI();
                // It's possible that we were waiting for this.user to be populated in order to post a
                // status update.
                handlePendingAction();
            }
        });


        profilePictureView = (ProfilePictureView) findViewById(R.id.profilePicture);
        greeting = (TextView) findViewById(R.id.greeting);

        postRegularPostButton = (Button) findViewById(R.id.regularPostUpdateButton);
        postRegularPostButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onClickPostRegularPost();
            }
        });

        postUnpublishedPostButton = (Button) findViewById(R.id.postUnpublishedPostButton);
        postUnpublishedPostButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onClickPostUnpublishedPost();
            }
        });

        listAllPostsButton = (Button) findViewById(R.id.listAllPostsButton);
        listAllPostsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onClickListAllPosts();
            }
        });

        showStatisticsButton = (Button) findViewById(R.id.showStatisticsButton);
        showStatisticsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onClickShowStatistics();
            }
        });

        controlsContainer = (ViewGroup) findViewById(R.id.main_ui_container);

        final FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            // If we're being re-created and have a fragment, we need to a) hide the main UI controls and
            // b) hook up its listeners again.
            controlsContainer.setVisibility(View.GONE);
            if (fragment instanceof FriendPickerFragment) {
                setFriendPickerListeners((FriendPickerFragment) fragment);
            } else if (fragment instanceof PlacePickerFragment) {
                setPlacePickerListeners((PlacePickerFragment) fragment);
            }
        }

        // Listen for changes in the back stack so we know if a fragment got popped off because the user
        // clicked the back button.
        fm.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if (fm.getBackStackEntryCount() == 0) {
                    // We need to re-show our UI.
                    controlsContainer.setVisibility(View.VISIBLE);
                }
            }
        });

        // Can we present the share dialog for regular links?
        canPresentShareDialog = FacebookDialog.canPresentShareDialog(this,
                FacebookDialog.ShareDialogFeature.SHARE_DIALOG);
        // Can we present the share dialog for photos?
        canPresentShareDialogWithPhotos = FacebookDialog.canPresentShareDialog(this,
                FacebookDialog.ShareDialogFeature.PHOTOS);

		Log.d("facebookfoo ###############", "OnCreate Exit!");
    }

    @Override
    protected void onResume() {
        super.onResume();
        uiHelper.onResume();

        // Call the 'activateApp' method to log an app event for use in analytics and advertising reporting.  Do so in
        // the onResume methods of the primary Activities that an app may be launched into.
        AppEventsLogger.activateApp(this);

        updateUI();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);

        outState.putString(PENDING_ACTION_BUNDLE_KEY, pendingAction.name());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data, dialogCallback);
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();

        // Call the 'deactivateApp' method to log an app event for use in analytics and advertising
        // reporting.  Do so in the onPause methods of the primary Activities that an app may be launched into.
        AppEventsLogger.deactivateApp(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        if (pendingAction != PendingAction.NONE &&
                (exception instanceof FacebookOperationCanceledException ||
                exception instanceof FacebookAuthorizationException)) {
                new AlertDialog.Builder(FacebookFooMainActivity.this)
                    .setTitle(R.string.cancelled)
                    .setMessage(R.string.permission_not_granted)
                    .setPositiveButton(R.string.ok, null)
                    .show();
            pendingAction = PendingAction.NONE;
        } else if (state == SessionState.OPENED_TOKEN_UPDATED) {
            handlePendingAction();
        }
        updateUI();
    }

    private void updateUI() {
        Session session = Session.getActiveSession();

		if(session != null) 
			userInfoSession = session;
		
        boolean enableButtons = (session != null && session.isOpened());

        postRegularPostButton.setEnabled(enableButtons);
        postUnpublishedPostButton.setEnabled(enableButtons);
        listAllPostsButton.setEnabled(enableButtons);
        showStatisticsButton.setEnabled(enableButtons);

        if (enableButtons && user != null) {
            profilePictureView.setProfileId(user.getId());
            greeting.setText(getString(R.string.hello_user, user.getFirstName()));
			retriveUserPages();
        } else {
            profilePictureView.setProfileId(null);
            greeting.setText(null);
        }
    }

    @SuppressWarnings("incomplete-switch")
    private void handlePendingAction() {
        PendingAction previouslyPendingAction = pendingAction;
        // These actions may re-set pendingAction if they are still pending, but we assume they
        // will succeed.
        pendingAction = PendingAction.NONE;

        switch (previouslyPendingAction) {
            case POST_PHOTO:
                postPhoto();
                break;
            case POST_PUBLISHED:
                postStatusUpdate(true);//[NJ] a published post
                break;
			case POST_UNPUBLISHED:
				postStatusUpdate(false);//[NJ] an unpublished post
				break;
	

        }
    }

    private interface GraphObjectWithId extends GraphObject {
        String getId();
    }

    private void showPublishResult(String message, GraphObject result, FacebookRequestError error) {
        String title = null;
        String alertMessage = null;
		
        if (error == null) {
            title = getString(R.string.success);
            String id = result.cast(GraphObjectWithId.class).getId();
            alertMessage = getString(R.string.successfully_posted_post, message, id);
        } else {
            title = getString(R.string.error);
            alertMessage = error.getErrorMessage();
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(alertMessage)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private void onClickPostRegularPost() {
        performPublish(PendingAction.POST_PUBLISHED, canPresentShareDialog);
    }

    private FacebookDialog.ShareDialogBuilder createShareDialogBuilderForLink() {
        return new FacebookDialog.ShareDialogBuilder(this)
                .setName("Hello Nature View FIG")
                .setDescription("The 'FacebookFooApp' application showcases what it needs to do for you")
                .setLink("http://developers.facebook.com/android");
    }

	//[NJ] Doing the actual job to make a request to "/{page_id}/feed" with intended published flag
	private void postStatusUpdate(boolean published){
		if (user != null && hasPublishPermission()) {
		   final String message = getString(R.string.status_update, user.getFirstName(), (new Date().toString()));
		   final String graphPath = page_id+"/feed";
		   //final String graphPath = "435557046594813/feed";
		   Request request = Request
				   .newStatusUpdateRequest(Session.getActiveSession(),graphPath, message, published, new Request.Callback() {
					   @Override
					   public void onCompleted(Response response) {
					       Log.d("facebookfoo ###############", "result: "+response.toString());
						   showPublishResult(message, response.getGraphObject(), response.getError());
					   }
				   });
		   request.executeAsync();
		}else {
            pendingAction = PendingAction.POST_PUBLISHED;
        }


	}

    private void onClickPostUnpublishedPost() {
        performPublish(PendingAction.POST_UNPUBLISHED, canPresentShareDialog);
    }

    private FacebookDialog.PhotoShareDialogBuilder createShareDialogBuilderForPhoto(Bitmap... photos) {
        return new FacebookDialog.PhotoShareDialogBuilder(this)
                .addPhotos(Arrays.asList(photos));
    }

    private void postPhoto() {
        Bitmap image = BitmapFactory.decodeResource(this.getResources(), R.drawable.icon);
        if (canPresentShareDialogWithPhotos) {
            FacebookDialog shareDialog = createShareDialogBuilderForPhoto(image).build();
            uiHelper.trackPendingDialogCall(shareDialog.present());
        } else if (hasPublishPermission()) {
            Request request = Request.newUploadPhotoRequest(Session.getActiveSession(), image, new Request.Callback() {
                @Override
                public void onCompleted(Response response) {
                    showPublishResult(getString(R.string.photo_post), response.getGraphObject(), response.getError());
                }
            });
            request.executeAsync();
        } else {
            pendingAction = PendingAction.POST_PHOTO;
        }
    }

    private void showPickerFragment(PickerFragment<?> fragment) {
        fragment.setOnErrorListener(new PickerFragment.OnErrorListener() {
            @Override
            public void onError(PickerFragment<?> pickerFragment, FacebookException error) {
                String text = getString(R.string.exception, error.getMessage());
                Toast toast = Toast.makeText(FacebookFooMainActivity.this, text, Toast.LENGTH_SHORT);
                toast.show();
            }
        });

        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();

        controlsContainer.setVisibility(View.GONE);

        // We want the fragment fully created so we can use it immediately.
        fm.executePendingTransactions();

        fragment.loadData(true);
    }

	private void showListPostsFragment(ListPostsFragment fragment){
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();

        controlsContainer.setVisibility(View.GONE);

        // We want the fragment fully created so we can use it immediately.
        fm.executePendingTransactions();



	}

	private void parsePagePosts(GraphObject result, FacebookRequestError error){
        String title = null;
        String alertMessage = null;

        if (error == null) {
			Log.d("facebookfoo ###############", "retrievig posts success");
			try {
			JSONArray page_posts = result.getInnerJSONObject().getJSONArray("data");

			 Log.d("facebookfoo ###############", "retrievig user posts length : "+page_posts.length());

			for (int i = 0; i < page_posts.length(); i++) {
				   Log.d("facebookfoo ##############", "post id : " +i );
				   JSONObject item = page_posts.getJSONObject(i);
				   String post_id = item.getString("id");
				   //Log.d("facebookfoo ##############", "post id : " + post_id);
				   String story = item.getString("story");
				   //Log.d("facebookfoo ##############","story : " + story);
				   String message = item.getString("type");
				   //Log.d("facebookfoo ##############","type : " + message);
				   String updated_time = item.getString("updated_time");
				  // Log.d("facebookfoo ##############","updated_time : " + updated_time);
				   //boolean ispublished = item.getString("")
				   if(!postsTable.containsKey(post_id)){
				   	 postsTable.put(post_id, new PostsRecord(post_id, story, message, updated_time, true));
				   }
				  
				   
				   
 				  
				  // Log.d("facebookfoo ##############","is_published : " + );
				   
				   
			
			   }
			} catch (Exception e) {}




        } else {
			Log.d("facebookfoo ###############", "retrievig page posts fail");
        }

	}

    private void onClickListAllPosts() {

	
		final ListPostsFragment fragment = new ListPostsFragment();

		fragment.setTargetPageID(page_id);
		
		fragment.setTitleText(getString(R.string.listpost_title));
		
		setListPostListeners(fragment);
		
		showPickerFragment(fragment);

    }

    private void setFriendPickerListeners(final FriendPickerFragment fragment) {
        fragment.setOnDoneButtonClickedListener(new FriendPickerFragment.OnDoneButtonClickedListener() {
            @Override
            public void onDoneButtonClicked(PickerFragment<?> pickerFragment) {
                onFriendPickerDone(fragment);
            }
        });
    }

    private void onFriendPickerDone(FriendPickerFragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStack();

        String results = "";

        List<GraphUser> selection = fragment.getSelection();
        tags = selection;
        if (selection != null && selection.size() > 0) {
            ArrayList<String> names = new ArrayList<String>();
            for (GraphUser user : selection) {
                names.add(user.getName());
            }
            results = TextUtils.join(", ", names);
        } else {
            results = getString(R.string.no_friends_selected);
        }

        showAlert(getString(R.string.you_picked), results);
    }

    private void onPlacePickerDone(PlacePickerFragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStack();

        String result = "";

        GraphPlace selection = fragment.getSelection();
        if (selection != null) {
            result = selection.getName();
        } else {
            result = getString(R.string.no_place_selected);
        }

        place = selection;

        showAlert(getString(R.string.you_picked), result);
    }


    private void onPostListDone(ListPostsFragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStack();

        String result = "";


    }

	private void setListPostListeners(final ListPostsFragment fragment) {
		 fragment.setOnDoneButtonClickedListener(new ListPostsFragment.OnDoneButtonClickedListener() {
			 @Override
			 public void onDoneButtonClicked(PickerFragment<?> pickerFragment) {
				 onPostListDone(fragment);
			 }
		 });
		 fragment.setOnSelectionChangedListener(new ListPostsFragment.OnSelectionChangedListener() {
			 @Override
			 public void onSelectionChanged(PickerFragment<?> pickerFragment) {
				 if (fragment.getSelection() != null) {
					 onPostListDone(fragment);
				 }
			 }
		 });
	 }


    private void onClickShowStatistics() {

		if(page_id == null) return;
		
		final String graphPath = page_id+"/promotable_posts";
		Request request = Request.newMyPageRequest(userInfoSession, graphPath, new Request.GraphUserCallback() {
			@Override
			public void onCompleted(GraphUser me,  Response response) {
		
				Log.d("facebookfoo ###############", "response "+ response.toString());
				parsePagePosts(response.getGraphObject(), response.getError());
			 
			}
		});
		Request.executeBatchAsync(request);

    }

	private void parseUserPages(GraphObject result, FacebookRequestError error){
        String title = null;
        String alertMessage = null;

        if (error == null) {
			Log.d("facebookfoo ###############", "retrievig user pages success");
			try {
			JSONArray user_pages = result.getInnerJSONObject().getJSONArray("data");

			for (int i = 0; i < user_pages.length(); i++) {
				   JSONObject item = user_pages.getJSONObject(i);
				   page_id = item.getString("id");
				   Log.d("facebookfoo ##############", "id : " + page_id);
				   Log.d("facebookfoo ##############","category : " + item.getString("category"));
				   Log.d("facebookfoo ##############","perms : " + item.getString("perms"));
 				   String accessToken = item.getString("access_token");
				   
				   if(accessToken != null){
                       Log.d("facebookfoo ##############","access_token : " + accessToken);
					   Session session= Session.getActiveSession();
					   AccessToken currentToken = session.getTokenInfo();
				      /*[NJ] Need to use the page access token in the session, with a couple of changes in the facebook sdk
 						1. make setTokenInfo public
 						2. Make AccessToken pubic
					   */
					   session.setTokenInfo(
									  new AccessToken(accessToken, currentToken.getExpires(), currentToken.getPermissions(),
											  currentToken.getDeclinedPermissions(), currentToken.getSource(), currentToken.getLastRefresh()));

					   
                    
				   	}
			
			   }
			} catch (Exception e) {}




        } else {
			Log.d("facebookfoo ###############", "retrievig user pages fail");
        }

	}


    private void retriveUserPages(){
		Log.d("facebookfoo ###############", "retrievig user pages");
		
		Request request = Request.newMyAccountsRequest(userInfoSession, new Request.GraphUserCallback() {
			@Override
			public void onCompleted(GraphUser me,  Response response) {
		
				Log.d("facebookfoo ###############", "response "+ response.toString());
			 
		        parseUserPages(response.getGraphObject(), response.getError());
			}
		});
		Request.executeBatchAsync(request);

	}
    private void setPlacePickerListeners(final PlacePickerFragment fragment) {
        fragment.setOnDoneButtonClickedListener(new PlacePickerFragment.OnDoneButtonClickedListener() {
            @Override
            public void onDoneButtonClicked(PickerFragment<?> pickerFragment) {
                onPlacePickerDone(fragment);
            }
        });
        fragment.setOnSelectionChangedListener(new PlacePickerFragment.OnSelectionChangedListener() {
            @Override
            public void onSelectionChanged(PickerFragment<?> pickerFragment) {
                if (fragment.getSelection() != null) {
                    onPlacePickerDone(fragment);
                }
            }
        });
    }

    private void showAlert(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private boolean hasPublishPermission() {
        Session session = Session.getActiveSession();
        return session != null && session.getPermissions().contains("publish_actions");
    }

    private void performPublish(PendingAction action, boolean allowNoSession) {
        Session session = Session.getActiveSession();
        if (session != null) {
			Log.d("facebookfoo #############", "session not null");
            pendingAction = action;
            if (hasPublishPermission()) {
                // We can do the action right away.
                handlePendingAction();
                return;
            } else if (session.isOpened()) {
                // We need to get new permissions, then complete the action when we get called back.
                session.requestNewPublishPermissions(new Session.NewPermissionsRequest(this, PERMISSION));
				
                return;
            }
        }

        if (allowNoSession) {
            pendingAction = action;
            handlePendingAction();
        }
    }
}
