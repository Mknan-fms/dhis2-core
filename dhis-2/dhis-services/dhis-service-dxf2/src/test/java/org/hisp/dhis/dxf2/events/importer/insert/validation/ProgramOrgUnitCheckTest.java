package org.hisp.dhis.dxf2.events.importer.insert.validation;

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

import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.RandomUtils;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.events.importer.validation.BaseValidationTest;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

/**
 * @author Luciano Fiandesio
 */
public class ProgramOrgUnitCheckTest extends BaseValidationTest
{
    private ProgramOrgUnitCheck rule;

    @Before
    public void setUp()
    {
        rule = new ProgramOrgUnitCheck();
    }

    @Test
    public void verifySuccessWhenProgramHasOrgUnitMatchingEventOrgUnit()
    {
        verifySuccessWhenProgramHasOrgUnitMatchingEventOrgUnit( "ABCDE", IdScheme.CODE );
        verifySuccessWhenProgramHasOrgUnitMatchingEventOrgUnit( CodeGenerator.generateUid(), IdScheme.UID );
        verifySuccessWhenProgramHasOrgUnitMatchingEventOrgUnit( "100", IdScheme.ID );
    }

    private void verifySuccessWhenProgramHasOrgUnitMatchingEventOrgUnit( String orgUnitId, IdScheme scheme )
    {
        // assign a UID to the event's org unit
        event.setOrgUnit( orgUnitId );

        // Prepare data
        Program program = createProgram( 'P' );
        // make sure that one of the generate Org Units, has the event's UID
        program.setOrganisationUnits( create( 5, event.getOrgUnit(), IdScheme.CODE ) );
        ProgramInstance pi = new ProgramInstance();
        pi.setProgram( program );

        Map<String, ProgramInstance> programInstanceMap = new HashMap<>();
        programInstanceMap.put( event.getUid(), pi );

        ImportOptions importOptions = ImportOptions.getDefaultImportOptions();
        importOptions.setOrgUnitIdScheme( IdScheme.CODE.name() );

        when( workContext.getImportOptions() ).thenReturn( importOptions );
        when( workContext.getProgramInstanceMap() ).thenReturn( programInstanceMap );

        // method under test
        ImportSummary summary = rule.check( new ImmutableEvent( event ), workContext );

        assertNoError( summary );
    }

    @Test
    public void failWhenProgramHasNoOrgUnitMatchingEventOrgUnit()
    {
        // assign a UID to the event's org unit
        event.setOrgUnit( CodeGenerator.generateUid() );

        // Prepare data
        Program program = createProgram( 'P' );
        // make sure that one of the generate Org Units, has the event's UID
        program.setOrganisationUnits( create( 5, CodeGenerator.generateUid(), IdScheme.UID ) );
        ProgramInstance pi = new ProgramInstance();
        pi.setProgram( program );

        Map<String, ProgramInstance> programInstanceMap = new HashMap<>();
        programInstanceMap.put( event.getUid(), pi );

        ImportOptions importOptions = ImportOptions.getDefaultImportOptions();

        when( workContext.getImportOptions() ).thenReturn( importOptions );
        when( workContext.getProgramInstanceMap() ).thenReturn( programInstanceMap );

        // method under test
        ImportSummary summary = rule.check( new ImmutableEvent( event ), workContext );

        assertHasError( summary, event, "Program is not assigned to this organisation unit: " + event.getOrgUnit() );
    }

    private Set<OrganisationUnit> create( int size, String orgUnit, IdScheme idScheme )
    {
        Set<OrganisationUnit> result = new HashSet<>();
        int rnd = RandomUtils.nextInt( 1, 5 );
        for ( int i = 0; i < size; i++ )
        {
            OrganisationUnit ou = createOrganisationUnit( RandomStringUtils.randomAlphabetic( 1 ) );
            if ( rnd == i )
            {
                if ( idScheme.equals( IdScheme.UID ) )
                {
                    ou.setUid( orgUnit );
                }
                else if ( idScheme.equals( IdScheme.CODE ) )
                {
                    ou.setCode( orgUnit );
                }
                else if ( idScheme.equals( IdScheme.ID ) )
                {
                    ou.setId( Long.parseLong( orgUnit ) );
                }
            }
            result.add( ou );
        }

        return result;
    }

}
