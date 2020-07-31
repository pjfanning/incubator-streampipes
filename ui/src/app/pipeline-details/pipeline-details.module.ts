/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import {NgModule} from "@angular/core";
import {FlexLayoutModule} from "@angular/flex-layout";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatTabsModule} from "@angular/material/tabs";
import {MatButtonModule} from "@angular/material/button";
import {CustomMaterialModule} from "../CustomMaterial/custom-material.module";
import {CommonModule} from "@angular/common";
import {MatProgressSpinnerModule} from "@angular/material/progress-spinner";
import {PipelineDetailsComponent} from "./pipeline-details.component";
import {PipelinePreviewComponent} from "./components/preview/pipeline-preview.component";
import {EditorModule} from "../editor/editor.module";
import {PipelineActionsComponent} from "./components/actions/pipeline-actions.component";
import {PipelineStatusComponent} from "./components/status/pipeline-status.component";
import {PipelineElementsComponent} from "./components/elements/pipeline-elements.component";
import {PipelineElementsRowComponent} from "./components/elements/pipeline-elements-row.component";
import {QuickEditComponent} from "./components/edit/quickedit.component";
import {ConnectModule} from "../connect/connect.module";

@NgModule({
  imports: [
    FlexLayoutModule,
    FormsModule,
    MatTabsModule,
    MatButtonModule,
    CustomMaterialModule,
    CommonModule,
    MatProgressSpinnerModule,
    EditorModule,
    ConnectModule,
    FormsModule,
    ReactiveFormsModule
  ],
  declarations: [
    PipelineActionsComponent,
    PipelineElementsComponent,
    PipelineElementsRowComponent,
    PipelineDetailsComponent,
    PipelineStatusComponent,
    PipelinePreviewComponent,
    QuickEditComponent
  ],
  providers: [],
  exports: [
    PipelineDetailsComponent
  ],
  entryComponents: [
    PipelineDetailsComponent
  ]
})
export class PipelineDetailsModule {

  constructor() {
  }

}