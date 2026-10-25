## Iteration 1
Files changed:
- src/main/java/com/storeops/activities/ActivityService.java - bulkStatus dedupes ids (LinkedHashSet, first-occurrence order), skips save and event for activities already in target status, publishes events only after all saves
- src/test/java/com/storeops/activities/BulkStatusTest.java - 8 new tests plus a status-probe listener
- README.md - endpoint table row for POST /api/activities/bulk-status
Criteria -> tests (BulkStatusTest):
- 1 -> duplicateIdsCountOnce; 2 -> duplicatesKeepFirstOccurrenceOrder
- 3 -> alreadyInTargetStatusIsUnchangedWithNoEvent; 4 -> mixOfChangedAndAlreadyDoneEmitsOneEventForTheChangedOne
- 5 -> doneToBlockedAndBlockedToTodoCarryPreviousAndNewStatus
- 6 -> duplicatesWithUnknownIdAreNotFoundAndChangeNothing; 7 -> nonManagerWithDuplicatesIsForbiddenAndChangesNothing
- 8 -> invalidBodiesAreValidationErrorsAndChangeNothing (existing, includes [])
- 9 -> README row (no test)
Carried forward: B4 README row; B3 duplicate tests above; nonManagerWithUnknownIdIsForbiddenStatusesUnchangedNoEvent (statuses unchanged, no event); eventsArePublishedOnlyAfterEveryActivityIsUpdated (probe sees all DONE at each event)
Events added/published: ActivityUpdatedEvent (existing) - ActivityService.bulkStatus, one per really changed activity, after all saves
Commands run:
- mvn -o -B verify - BUILD SUCCESS, 34 tests, 0 failures, Checkstyle and SpotBugs clean
- mvn -o -B test -Dtest=ArchitectureRulesTest - BUILD SUCCESS, 6 tests
Fixes for previous feedback: n/a (iteration 1 of sprint 2; carried-forward items addressed above)
Concerns: none. Sprint 1 concern remains: @Valid runs before the role check, so a non-manager with an invalid body gets 400.
