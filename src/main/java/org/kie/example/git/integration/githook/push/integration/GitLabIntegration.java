/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.example.git.integration.githook.push.integration;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.kie.example.git.integration.githook.push.properties.GitRemoteProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitLabIntegration implements GitRemoteIntegration {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitLabIntegration.class);

    private static final String ERROR_LOGIN = "Connecting using username and password is not supported with gitlab, kindly use token instead.";

    private GitLabApi gitlab;
    private CredentialsProvider credentialsProvider;
    private int groupId = -1;

    public GitLabIntegration(final GitRemoteProperties props) {
        if (props.getToken().isEmpty()) {
            LOGGER.error(ERROR_LOGIN);
            throw new RuntimeException(ERROR_LOGIN);
        } else {
            LOGGER.info("Connecting using token");
            gitlab = new GitLabApi(props.getRemoteGitUrl(), props.getToken());
            //Added null check for optional gitlab groupId property
            if (props.getGitLabGroup() != null)
                groupId = resolveGroupId(props.getGitLabGroup());
            credentialsProvider = new UsernamePasswordCredentialsProvider("", props.getToken());
        }
    }

    @Override
    public String createRepository(String repoName) {
        try {
            if (groupId >= 0) {
                return gitlab.getProjectApi().createProject(groupId, repoName).getHttpUrlToRepo();
            }
            return gitlab.getProjectApi().createProject(repoName).getHttpUrlToRepo();
        } catch (GitLabApiException e) {
            LOGGER.error("An unexpected error occurred.", e);
            throw new RuntimeException(e);
        }
    }

    public int resolveGroupId(final String groupPath) {
        if (!groupPath.trim().isEmpty()) {
            try {
                return gitlab.getGroupApi().getGroup(groupPath).getId();
            } catch (GitLabApiException e) {
                throw new GroupNotFoundException("Group \"" + groupPath + "\" is not found");
            }
        }
        return -1;
    }

    @Override
    public CredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }
}
