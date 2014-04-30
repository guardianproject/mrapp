package info.guardianproject.mrapp.publish;

import java.util.Arrays;
import java.util.List;

import info.guardianproject.mrapp.model.Job;
import info.guardianproject.mrapp.model.JobTable;
import info.guardianproject.mrapp.model.Project;
import info.guardianproject.mrapp.model.PublishJob;
import info.guardianproject.mrapp.publish.sites.StoryMakerPublisher;
import org.holoeverywhere.app.Activity;
import android.util.Log;

// TODO we need to make sure this will be thread safe since upload and render jobs are on separate threads and could callback in a race here

/**
 * 
 * @author Josh Steiner <josh@vitriolix.com>
 *
 */
public class PublishController {
    private final String TAG = "PublishController";
    
	private static PublishController publishController = null;
	private Activity mActivity;
	UploadService uploadService;
	RenderService renderService;
	PublisherBase publisher;
	PublishJob publishJob;
	PublishListener mListener;
	
	public PublishController(Activity activity, PublishListener listener) {
	    mActivity = activity;
	    mListener = listener;
	}

	public static PublishController getInstance(Activity activity, PublishListener listener) {
		if (publishController == null) {
			publishController = new PublishController(activity, listener);
		}
		
		return publishController;
	}
	
	// FIXME this won't help us get more than one publisher per run
	public PublisherBase getPublisher(PublishJob publishJob) {
		String[] keys = publishJob.getSiteKeys();
		List<String> ks = Arrays.asList(keys);
		if (ks.contains("storymaker")) {
			publisher = new StoryMakerPublisher(mActivity, this, publishJob);
		}
		// TODO add others
		
		return publisher;
	}
	
	public void startPublish(Project project, String[] siteKeys) {
		publishJob = new PublishJob(mActivity, -1, project.getId(), siteKeys);
		publishJob.save();
		getPublisher(publishJob).start();
		startRenderService();
		startUploadService();
	}
	
	public void publishJobSucceeded(PublishJob publishJob) {
		mListener.publishSucceeded(publishJob);
	}
	
	public void jobSucceeded(Job job, String code) {
        Log.d(TAG, "jobSucceeded: " + job + ", with code: " + code);
		// TODO need to raise this to the interested activities here
		getPublisher(job.getPublishJob()).jobSucceeded(job);
	}
	
	public void jobFailed(Job job, int errorCode, String errorMessage) {
        Log.d(TAG, "jobFailed: " + job + ", with errorCode: " + errorCode + ", and errorMessage: " + errorMessage);
		// TODO need to raise this to the interested activities here
		getPublisher(job.getPublishJob()).jobFailed(job);
	}
	
	private void startUploadService() {
		uploadService = UploadService.getInstance(mActivity, this);
		uploadService.start();
	}
	
	private void startRenderService() {
		renderService = RenderService.getInstance(mActivity, this);
		renderService.start();
	}
	
	public void enqueueJob(Job job) {
		job.setQueuedAtNow();
		job.save();
		if (job.isType(JobTable.TYPE_UPLOAD)) {
			startUploadService();
		} else if (job.isType(JobTable.TYPE_RENDER)) {
			startRenderService();
		}
	}
	
	public static interface PublishListener {
	    public void publishSucceeded(PublishJob publishJob);
	    
//	    public void publishFailed(PublishJob publishJob);
	}

}