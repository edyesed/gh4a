package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.Review;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;

public class ReviewLoader extends BaseLoader<Review> {

    private final String mRepoOwner;
    private final String mRepoName;
    private final int mPullRequestNumber;
    private final int mReviewNumber;

    public ReviewLoader(Context context, String repoOwner, String repoName, int pullRequestNumber, int reviewNumber) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPullRequestNumber = pullRequestNumber;
        mReviewNumber = reviewNumber;
    }

    @Override
    public Review doLoadInBackground() throws IOException {
        return loadReviewRequest(mRepoOwner, mRepoName, mPullRequestNumber, mReviewNumber);
    }

    public static Review loadReviewRequest(String repoOwner, String repoName,
            int pullRequestNumber, int reviewNumber) throws IOException {
        PullRequestService pullRequestService = (PullRequestService)
                Gh4Application.get().getService(Gh4Application.PULL_SERVICE);
        return pullRequestService.getReview(new RepositoryId(repoOwner, repoName),
                pullRequestNumber, reviewNumber);
    }
}
