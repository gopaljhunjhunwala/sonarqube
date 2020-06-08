/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.issue.index;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.es.EsIndexSyncInProgressException;

public class IssueIndexSyncProgressChecker {
  private final DbClient dbClient;

  public IssueIndexSyncProgressChecker(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public IssueSyncProgress getIssueSyncProgress(DbSession dbSession) {
    int completed = dbClient.branchDao().countByNeedIssueSync(dbSession, false);
    int total = dbClient.branchDao().countAll(dbSession);
    return new IssueSyncProgress(completed, total);
  }

  public void checkIfAnyComponentsIssueSyncInProgress(DbSession dbSession, List<String> componentKeys, @Nullable String branch,
    @Nullable String pullRequest) {
    boolean needIssueSync = dbClient.branchDao().doAnyOfComponentsNeedIssueSync(dbSession, componentKeys, branch, pullRequest);
    if (needIssueSync) {
      throw new EsIndexSyncInProgressException(IssueIndexDefinition.TYPE_ISSUE.getMainType(),
          "Results are temporarily unavailable. Indexing of issues is in progress.");
    }
  }

  /**
   * Checks if issue index sync is in progress, if it is, method throws exception org.sonar.server.es.EsIndexSyncInProgressException
   */
  public void checkIfIssueSyncInProgress(DbSession dbSession) {
    if (isIssueSyncInProgress(dbSession)) {
      throw new EsIndexSyncInProgressException(IssueIndexDefinition.TYPE_ISSUE.getMainType(),
          "Results are temporarily unavailable. Indexing of issues is in progress.");
    }
  }

  public boolean isIssueSyncInProgress(DbSession dbSession) {
    return dbClient.branchDao().hasAnyBranchWhereNeedIssueSync(dbSession, true);
  }

  public boolean doProjectNeedIssueSync(DbSession dbSession, String projectUuid) {
    return !findProjectUuidsWithIssuesSyncNeed(dbSession, Sets.newHashSet(projectUuid)).isEmpty();
  }

  public List<String> findProjectUuidsWithIssuesSyncNeed(DbSession dbSession, Collection<String> projectUuids) {
    return dbClient.branchDao().selectProjectUuidsWithIssuesNeedSync(dbSession, projectUuids);
  }
}
