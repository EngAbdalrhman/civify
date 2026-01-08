package com.app.civify.usecase.admin;

import com.app.civify.dto.group.GroupCreateRequest;
import com.app.civify.dto.group.GroupResponse;
import com.app.civify.util.filter.InputDataModel;
import com.app.civify.util.filter.PagedResponse;

import java.util.List;

/**
 * Use case interface for Admin Group management.
 */
public interface AdminGroupUseCase {

    /**
     * Filter groups based on search criteria.
     *
     * @param inputData filter criteria (page, size, sort, search)
     * @return paged list of groups
     */
    PagedResponse<GroupResponse> filterGroups(InputDataModel inputData);

    /**
     * Get all groups.
     *
     * @return list of all groups
     */
    List<GroupResponse> getAllGroups();

    /**
     * Get all available permissions.
     *
     * @return list of permissions
     */
    List<GroupResponse.PermissionDto> getAllPermissions();

    /**
     * Get group by ID.
     *
     * @param groupId the group ID
     * @return group details
     */
    GroupResponse getGroupById(Long groupId);

    /**
     * Create a new group.
     *
     * @param request group creation details
     * @return created group
     */
    GroupResponse createGroup(GroupCreateRequest request);

    /**
     * Update an existing group.
     *
     * @param groupId the group ID
     * @param request update details
     * @return updated group
     */
    GroupResponse updateGroup(Long groupId, GroupCreateRequest request);

    /**
     * Delete a group.
     *
     * @param groupId the group ID
     */
    void deleteGroup(Long groupId);

    /**
     * Toggle group active status.
     *
     * @param groupId the group ID
     * @return updated group
     */
    GroupResponse toggleGroupActive(Long groupId);
}
