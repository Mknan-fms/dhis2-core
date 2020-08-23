package org.hisp.dhis.tracker.bundle;

/*
 * Copyright (c) 2004-2020, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.report.TrackerBundleReport;
import org.hisp.dhis.tracker.report.TrackerStatus;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Zubair Asghar
 */
public class TrackerObjectDeletionServiceTest  extends DhisSpringTest
{
    @Autowired
    private ObjectBundleService objectBundleService;

    @Autowired
    private ObjectBundleValidationService objectBundleValidationService;

    @Autowired
    private RenderService _renderService;

    @Autowired
    private UserService _userService;

    @Autowired
    private TrackerBundleService trackerBundleService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Override
    protected void setUpTest() throws IOException
    {
        preCreateInjectAdminUserWithoutPersistence();

        renderService = _renderService;
        userService = _userService;

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "tracker/tracker_basic_metadata.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validationReport = objectBundleValidationService.validate( bundle );

        assertTrue( validationReport.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );

        TrackerBundle trackerBundle = renderService
            .fromJson( new ClassPathResource("tracker/tracker_basic_data_before_deletion.json").getInputStream(),
                TrackerBundleParams.class ).toTrackerBundle();

        assertEquals( 13, trackerBundle.getTrackedEntities().size() );
        assertEquals( 1, trackerBundle.getEnrollments().size() );

        List<TrackerBundle> trackerBundles = trackerBundleService.create( TrackerBundleParams.builder()
            .trackedEntities( trackerBundle.getTrackedEntities() )
            .enrollments( trackerBundle.getEnrollments() )
            .build() );

        assertEquals( 1, trackerBundles.size() );

        TrackerBundleReport bundleReport = trackerBundleService.commit( trackerBundles.get( 0 ) );

        assertEquals( bundleReport.getTypeReportMap().get( TrackerType.TRACKED_ENTITY ).getStats().getCreated(), manager.getAll( TrackedEntityInstance.class ).size() );
        assertEquals( 3, manager.getAll( ProgramInstance.class ).size() );
    }

    @Test
    public void testTrackedEntityInstanceDeletion() throws IOException
    {
        TrackerBundle trackerBundle = renderService
            .fromJson( new ClassPathResource( "tracker/tracked_entity_basic_data_for_deletion.json" ).getInputStream(),
            TrackerBundleParams.class ).toTrackerBundle();

        assertEquals( 9, trackerBundle.getTrackedEntities().size() );

        List<TrackerBundle> trackerBundles = trackerBundleService.create( TrackerBundleParams.builder()
            .trackedEntities( trackerBundle.getTrackedEntities() )
            .build() );

        assertEquals( 1, trackerBundles.size() );

        TrackerBundleReport bundleReport = trackerBundleService.delete( trackerBundles.get( 0 ) );

        assertEquals( bundleReport.getStatus(), TrackerStatus.OK );
        assertTrue( bundleReport.getTypeReportMap().containsKey( TrackerType.TRACKED_ENTITY ) );
        assertEquals( bundleReport.getTypeReportMap().get( TrackerType.TRACKED_ENTITY ).getStats().getDeleted(), 9 );

        // remaining
        assertEquals( 4, manager.getAll( TrackedEntityInstance.class ).size() );
    }

    @Test
    public void testEnrollmentDeletion() throws IOException
    {
        TrackerBundle trackerBundle = renderService
            .fromJson( new ClassPathResource( "tracker/enrollment_basic_data_for_deletion.json" ).getInputStream(),
                    TrackerBundleParams.class ).toTrackerBundle();

        List<TrackerBundle> trackerBundles = trackerBundleService.create( TrackerBundleParams.builder()
            .enrollments( trackerBundle.getEnrollments() )
            .build() );

        assertEquals( 1, trackerBundles.size() );

        TrackerBundleReport bundleReport = trackerBundleService.delete( trackerBundles.get( 0 ) );

        assertEquals( bundleReport.getStatus(), TrackerStatus.OK );
        assertTrue( bundleReport.getTypeReportMap().containsKey( TrackerType.ENROLLMENT ) );
        assertEquals( bundleReport.getTypeReportMap().get( TrackerType.ENROLLMENT ).getStats().getDeleted(), 1 );

        // remaining
        assertEquals( 2, manager.getAll( ProgramInstance.class ).size() );
    }
}