## Iteration 1
Files changed:
- src/main/java/com/storeops/activities/BulkStatusRequest.java - new record, @NotEmpty activityIds, @NotNull status
- src/main/java/com/storeops/activities/ActivityController.java - POST /bulk-status, @Valid and routing only
- src/main/java/com/storeops/activities/ActivityService.java - bulkStatus: manager check, then ID resolution (first unknown -> NotFoundError), then update loop, then events
- src/test/java/com/storeops/activities/BulkStatusTest.java - MockMvc and event tests
Criteria -> tests (BulkStatusTest):
- 1, 2, 3 -> managerUpdatesAllInOrderAndPublishesOneEventEach
- 4, 5, 6 -> nonManagerMissingHeaderAndUnknownStaffAreForbidden
- 7 -> permissionIsCheckedBeforeIds
- 8, 9 -> unknownIdIsNotFoundNamingFirstUnknownAndChangesNothing
- 10, 11, 12 -> invalidBodiesAreValidationErrorsAndChangeNothing
- 13 -> anyStatusToAnyOtherSucceeds
Events added/published: ActivityUpdatedEvent (existing) - ActivityService.bulkStatus, one per activity, after all saves
Commands run:
- mvn -o -B verify - BUILD SUCCESS, 25 tests, 0 failures, Checkstyle and SpotBugs clean
- mvn -o -B test -Dtest=ArchitectureRulesTest - BUILD SUCCESS, 6 tests
Fixes for previous feedback: n/a (iteration 1)
Concerns: Validation (@Valid) runs before the service role check, so a non-manager with an invalid body gets 400, not 403. The contract does not specify this case. Duplicate IDs would emit two events (out of scope, sprint 2). Events are published in request order.
