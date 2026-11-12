package no.hvl.dat250.lms.group.api.controllers;

import no.hvl.dat250.lms.group.api.dto.GroupDTO;
import no.hvl.dat250.lms.group.domain.Group;
import no.hvl.dat250.lms.group.service.GroupService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupControllerTest {

    private static final Long COURSE_ID = 10L;
    private static final Long GROUP_ID = 5L;
    private static final Long USER_ID = 30L;

    @Mock
    private GroupService service;

    @InjectMocks
    private GroupController controller;

    // ---------------------------------------------------------
    // GET ALL GROUPS
    // ---------------------------------------------------------

    @Test
    void getAllGroupsDelegatesToService() {
        GroupDTO dto = new GroupDTO(GROUP_ID, 2, Set.of());

        when(service.getAllGroups(COURSE_ID))
                .thenReturn(List.of(dto));

        var result = controller.getAllGroups(COURSE_ID);

        assertEquals(List.of(dto), result);

        verify(service).getAllGroups(COURSE_ID);
    }

    // ---------------------------------------------------------
    // GET GROUP BY ID
    // ---------------------------------------------------------

    @Test
    void getGroupDelegatesToService() {
        GroupDTO dto = new GroupDTO(GROUP_ID, 2, Set.of());

        when(service.getGroupById(COURSE_ID, GROUP_ID))
                .thenReturn(dto);

        var result = controller.getGroup(COURSE_ID, GROUP_ID);

        assertEquals(dto, result);

        verify(service).getGroupById(COURSE_ID, GROUP_ID);
    }

    // ---------------------------------------------------------
    // CREATE GROUP
    // ---------------------------------------------------------

    @Test
    void createGroupReturns201AndLocation() {
        var request = new MockHttpServletRequest(
                "POST",
                "/api/courses/10/groups"
        );

        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(request)
        );

        try {
            Group input = new Group(2, null);

            GroupDTO dto = new GroupDTO(
                    GROUP_ID,
                    2,
                    Set.of()
            );

            when(service.createGroup(COURSE_ID, 2))
                    .thenReturn(dto);

            var response = controller.createGroup(COURSE_ID, input);

            assertEquals(
                    HttpStatus.CREATED,
                    response.getStatusCode()
            );

            assertEquals(dto, response.getBody());

            assertNotNull(response.getHeaders().getLocation());

            assertEquals(
                    "/api/courses/10/groups/5",
                    response.getHeaders().getLocation().getPath()
            );

            verify(service).createGroup(COURSE_ID, 2);

        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    // ---------------------------------------------------------
    // ADD GROUP MEMBER
    // ---------------------------------------------------------

    @Test
    void addMemberReturnsUpdatedGroup() {
        GroupDTO dto = new GroupDTO(
                GROUP_ID,
                2,
                Set.of(USER_ID)
        );

        when(service.addGroupMember(COURSE_ID, GROUP_ID, USER_ID))
                .thenReturn(dto);

        var response = controller.addGroupMember(
                COURSE_ID,
                GROUP_ID,
                USER_ID
        );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertEquals(dto, response.getBody());

        verify(service).addGroupMember(
                COURSE_ID,
                GROUP_ID,
                USER_ID
        );
    }

    // ---------------------------------------------------------
    // DELETE GROUP
    // ---------------------------------------------------------

    @Test
    void deleteGroupPassesIdsInCorrectOrder() {
        var response = controller.deleteGroup(
                COURSE_ID,
                GROUP_ID
        );

        assertEquals(
                HttpStatus.NO_CONTENT,
                response.getStatusCode()
        );

        verify(service).deleteGroup(
                COURSE_ID,
                GROUP_ID
        );
    }

    // ---------------------------------------------------------
    // REMOVE GROUP MEMBER
    // ---------------------------------------------------------

    @Test
    void deleteMemberReturns204() {
        var response = controller.deleteUserFromGroup(
                COURSE_ID,
                GROUP_ID,
                USER_ID
        );

        assertEquals(
                HttpStatus.NO_CONTENT,
                response.getStatusCode()
        );

        verify(service).removeGroupMember(
                COURSE_ID,
                GROUP_ID,
                USER_ID
        );
    }
}