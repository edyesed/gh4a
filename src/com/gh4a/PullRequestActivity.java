/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryCommit;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.adapter.CommentAdapter;
import com.gh4a.loader.IssueCommentsLoader;
import com.gh4a.loader.PullRequestLoader;
import com.gh4a.loader.RepositoryCommitsLoader;
import com.gh4a.utils.CommitUtils;
import com.gh4a.utils.ImageDownloader;
import com.gh4a.utils.StringUtils;

public class PullRequestActivity extends BaseSherlockFragmentActivity
    implements LoaderManager.LoaderCallbacks {

    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;
    private LinearLayout mHeader;
    private CommentAdapter mCommentAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.pull_request);
        setUpActionBar();

        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);
        mPullRequestNumber = data.getInt(Constants.PullRequest.NUMBER);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.pull_request_title) + " #" + mPullRequestNumber);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        getSupportLoaderManager().initLoader(0, null, this);
        getSupportLoaderManager().getLoader(0).forceLoad();
        
        getSupportLoaderManager().initLoader(1, null, this);
        getSupportLoaderManager().initLoader(2, null, this);
    }

    private void fillData(final PullRequest pullRequest) {
        getSupportLoaderManager().getLoader(1).forceLoad();
        getSupportLoaderManager().getLoader(2).forceLoad();
        
        ListView lvComments = (ListView) findViewById(R.id.list_view);
        
        // set details inside listview header
        LayoutInflater infalter = getLayoutInflater();
        mHeader = (LinearLayout) infalter.inflate(R.layout.pull_request_header, lvComments, false);
        mHeader.setClickable(false);
        lvComments.addHeaderView(mHeader, null, true);

        mCommentAdapter = new CommentAdapter(PullRequestActivity.this, new ArrayList<Comment>());
        lvComments.setAdapter(mCommentAdapter);
        
        ImageView ivGravatar = (ImageView) mHeader.findViewById(R.id.iv_gravatar);
        if (pullRequest.getUser() != null) {
            ImageDownloader.getInstance().download(pullRequest.getUser().getGravatarId(),
                    ivGravatar);
            ivGravatar.setOnClickListener(new OnClickListener() {
    
                @Override
                public void onClick(View arg0) {
                    getApplicationContext()
                            .openUserInfoActivity(PullRequestActivity.this,
                                    pullRequest.getUser().getLogin(),
                                    pullRequest.getUser().getName());
                }
            });
        }
        
        TextView tvExtra = (TextView) mHeader.findViewById(R.id.tv_extra);
        TextView tvState = (TextView) mHeader.findViewById(R.id.tv_state);
        TextView tvTitle = (TextView) mHeader.findViewById(R.id.tv_title);
        
        TextView tvDesc = (TextView) mHeader.findViewById(R.id.tv_desc);
        TextView tvCommentTitle = (TextView) mHeader.findViewById(R.id.comment_title);
        tvCommentTitle.setTypeface(getApplicationContext().boldCondensed);
        tvCommentTitle.setTextColor(Color.parseColor("#0099cc"));
        tvCommentTitle.setText(getResources().getString(R.string.pull_request_comments) + " (" + pullRequest.getComments() + ")");
        
        tvState.setText(pullRequest.getState());
        tvState.setTextColor(Color.WHITE);
        if ("closed".equals(pullRequest.getState())) {
            tvState.setBackgroundResource(R.drawable.default_red_box);
            tvState.setText("C\nL\nO\nS\nE\nD");
        }
        else {
            tvState.setBackgroundResource(R.drawable.default_green_box);
            tvState.setText("O\nP\nE\nN");
        }
        tvTitle.setText(pullRequest.getTitle());
        tvTitle.setTypeface(getApplicationContext().boldCondensed);
        
        String body = pullRequest.getBody();
        if (!StringUtils.isBlank(body)) {
            body = body.replaceAll("\n", "<br/>");
            tvDesc.setText(Html.fromHtml(body));
            tvDesc.setTypeface(getApplicationContext().regular);
        }
        tvExtra.setText(getResources().getString(R.string.issue_open_by_user,
                pullRequest.getUser() != null ? pullRequest.getUser().getLogin() : "",
                pt.format(pullRequest.getCreatedAt())));
        
    }
    
    private void fillCommits(List<RepositoryCommit> commits) {
        LinearLayout llCommits = (LinearLayout) findViewById(R.id.ll_commits);
        for (final RepositoryCommit commit : commits) {
            LinearLayout rowView = new LinearLayout(this);
            rowView.setOrientation(LinearLayout.VERTICAL);
            rowView.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
            rowView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            rowView.setPadding(0, 10, 0, 10);
            
            TextView tvName = new TextView(this);
            tvName.setText(CommitUtils.getAuthorLogin(commit) + " added a commit");
            tvName.setTextAppearance(getApplicationContext(), android.R.attr.textAppearanceMedium);
            rowView.addView(tvName);
            
            TextView tvLabel = new TextView(this);
            tvLabel.setSingleLine(true);
            tvLabel.setText(commit.getSha().subSequence(0, 7) + " " + commit.getCommit().getMessage());
            tvLabel.setTextColor(Color.parseColor("#0099cc"));
            
            rowView.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View arg0) {
                    getApplicationContext().openCommitInfoActivity(PullRequestActivity.this, mRepoOwner,
                            mRepoName, commit.getSha(), 0);
                }
            });

            rowView.addView(tvLabel);
            
            llCommits.addView(rowView);
        }
    }
    private void fillDiscussion(List<Comment> comments) {
        if (comments != null && comments.size() > 0) {
            mCommentAdapter.notifyDataSetChanged();
            for (Comment comment : comments) {
                mCommentAdapter.add(comment);
            }
        }
        mCommentAdapter.notifyDataSetChanged();
    }

    @Override
    public Loader onCreateLoader(int id, Bundle arg1) {
        if (id == 0) {
            return new PullRequestLoader(this, mRepoOwner, mRepoName, mPullRequestNumber);
        }
        else if (id == 1) {
            return new IssueCommentsLoader(this, mRepoOwner, mRepoName, mPullRequestNumber);
        }
        else {
            return new RepositoryCommitsLoader(this, mRepoOwner, mRepoName, mPullRequestNumber);
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object object) {
        if (loader.getId() == 0) {
            hideLoading();
            fillData((PullRequest) object);
        }
        else if (loader.getId() == 1) {
            fillDiscussion((List<Comment>) object);
        }
        else {
            fillCommits((List<RepositoryCommit>) object);
        }
    }

    @Override
    public void onLoaderReset(Loader arg0) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            getApplicationContext().openPullRequestListActivity(this, mRepoOwner, mRepoName,
                    Constants.Issue.ISSUE_STATE_OPEN, Intent.FLAG_ACTIVITY_CLEAR_TOP);
            return true;
        default:
            return true;
        }
    }
}
