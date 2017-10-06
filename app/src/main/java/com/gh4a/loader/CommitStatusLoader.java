package com.gh4a.loader;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.content.Context;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.Status;
import com.meisolsson.githubsdk.model.StatusState;
import com.meisolsson.githubsdk.service.repositories.RepositoryStatusService;

public class CommitStatusLoader extends BaseLoader<List<Status>> {
    private final String mRepoOwner;
    private final String mRepoName;
    private final String mSha;

    private static final Comparator<Status> TIMESTAMP_COMPARATOR = new Comparator<Status>() {
        @Override
        public int compare(Status lhs, Status rhs) {
            return rhs.updatedAt().compareTo(lhs.updatedAt());
        }
    };

    private static final Comparator<Status> STATUS_AND_CONTEXT_COMPARATOR = new Comparator<Status>() {
        @Override
        public int compare(Status lhs, Status rhs) {
            int lhsSeverity = getStateSeverity(lhs);
            int rhsSeverity = getStateSeverity(rhs);
            if (lhsSeverity != rhsSeverity) {
                return lhsSeverity < rhsSeverity ? 1 : -1;
            } else {
                return lhs.context().compareTo(rhs.context());
            }
        }

        private int getStateSeverity(Status status) {
            switch (status.state()) {
                case Success: return 0;
                case Error:
                case Failure: return 2;
                default: return 1;
            }
        }
    };

    public CommitStatusLoader(Context context, String repoOwner, String repoName, String sha) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mSha = sha;
    }

    @Override
    public List<Status> doLoadInBackground() throws IOException {
        final RepositoryStatusService service =
                Gh4Application.get().getGitHubService(RepositoryStatusService.class);
        List<Status> statuses = ApiHelpers.Pager.fetchAllPages(new ApiHelpers.Pager.PageProvider<Status>() {
            @Override
            public Page<Status> providePage(long page) throws IOException {
                return ApiHelpers.throwOnFailure(
                        service.getStatuses(mRepoOwner, mRepoName, mSha, page).blockingGet());
            }
        });

        // Sort by timestamps first, so the removal logic below keeps the newest status
        Collections.sort(statuses, TIMESTAMP_COMPARATOR);

        // Filter out outdated statuses, only keep the newest status per context
        Set<String> seenContexts = new HashSet<>();
        Iterator<Status> iter = statuses.iterator();
        while (iter.hasNext()) {
            Status status = iter.next();
            if (seenContexts.contains(status.context())) {
                iter.remove();
            } else {
                seenContexts.add(status.context());
            }
        }

        // sort by status, then context
        Collections.sort(statuses, STATUS_AND_CONTEXT_COMPARATOR);

        return statuses;
    }
}
