/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;
import org.jkiss.dbeaver.registry.DataSourceNavigatorSettings;
import org.jkiss.dbeaver.registry.DataSourceProviderDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverSelectViewer;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverTreeViewer;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.CommonUtils;

/**
 * Driver selection page
 * step1
 */
class ConnectionPageDriver extends ActiveWizardPage implements ISelectionChangedListener, IDoubleClickListener {

    private static final String DEFAULT_NAVIGATOR_SETTINGS_RESET = "navigator.settings.preset.default";

    private NewConnectionWizard wizard;
    private DBPDriver selectedDriver;
    private DataSourceNavigatorSettings.Preset navigatorPreset;
    private DriverSelectViewer driverSelectViewer;
    private ProjectSelectorPanel projectSelector;

    ConnectionPageDriver(NewConnectionWizard wizard)
    {
        super("newConnectionDrivers");
        this.wizard = wizard;
        setTitle(CoreMessages.dialog_new_connection_wizard_start_title);
        setDescription(CoreMessages.dialog_new_connection_wizard_start_description);

        String defPreset = DBeaverActivator.getInstance().getPreferences().getString(DEFAULT_NAVIGATOR_SETTINGS_RESET);
        if (CommonUtils.isEmpty(defPreset)) {
            defPreset = DataSourceNavigatorSettings.PRESET_FULL.getId();
        }

        for (DataSourceNavigatorSettings.Preset p : DataSourceNavigatorSettings.PRESETS.values()) {
            if (p.getId().equals(defPreset)) {
                navigatorPreset = p;
                break;
            }
        }
        if (navigatorPreset == null) {
            navigatorPreset = DataSourceNavigatorSettings.PRESET_FULL;
        }
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite placeholder = UIUtils.createComposite(parent, 1);

        setControl(placeholder);

        Composite controlsGroup = UIUtils.createComposite(placeholder, 5);
        controlsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Navigator view preset
        {
            Composite presetComposite = new Composite(controlsGroup, SWT.NONE);
            presetComposite.setLayout(new RowLayout());
            new Label(presetComposite, SWT.NONE).setImage(DBeaverIcons.getImage(UIIcon.CONFIGURATION));
            new Label(presetComposite, SWT.NONE).setText("Connection view:  ");
            for (DataSourceNavigatorSettings.Preset p : DataSourceNavigatorSettings.PRESETS.values()) {
                if (p != DataSourceNavigatorSettings.PRESET_CUSTOM) {
                    Button pButton = new Button(presetComposite, SWT.RADIO);
                    pButton.setText(p.getName());
                    pButton.setToolTipText(p.getDescription());
                    if (p == navigatorPreset) {
                        pButton.setSelection(true);
                    }
                    pButton.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            if (pButton.getSelection()) {
                                navigatorPreset = p;
                                DBeaverActivator.getInstance().getPreferences().setValue(DEFAULT_NAVIGATOR_SETTINGS_RESET, navigatorPreset.getId());
                            }
                        }
                    });
                }
            }
        }

        {
            // Spacer
            createPanelDivider(controlsGroup);
        }

        {
            // Sorter
            Composite orderGroup = new Composite(controlsGroup, SWT.NONE);
            orderGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
            orderGroup.setLayout(new RowLayout());
            new Label(orderGroup, SWT.NONE).setImage(DBeaverIcons.getImage(UIIcon.SORT));
            new Label(orderGroup, SWT.NONE).setText("Sort by: ");
            DriverSelectViewer.OrderBy defaultOrderBy = DriverSelectViewer.getDefaultOrderBy();

            for (DriverSelectViewer.OrderBy ob : DriverSelectViewer.OrderBy.values()) {
                Button obScoreButton = new Button(orderGroup, SWT.RADIO);
                obScoreButton.setText(ob.getLabel());
                obScoreButton.setToolTipText(ob.getDescription());
                obScoreButton.setData(ob);
                if (ob == defaultOrderBy) {
                    obScoreButton.setSelection(true);
                }
                obScoreButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        driverSelectViewer.setOrderBy(
                            (DriverSelectViewer.OrderBy) obScoreButton.getData());
                    }
                });
            }
        }

        {
            createPanelDivider(controlsGroup);
        }

        projectSelector = new ProjectSelectorPanel(controlsGroup, NavigatorUtils.getSelectedProject());
        if (projectSelector.getSelectedProject() == null) {
            setErrorMessage("You need to create a project first");
        }

        {
            driverSelectViewer = new DriverSelectViewer(placeholder, this, wizard.getAvailableProvides(), true);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 200;
            driverSelectViewer.getControl().setLayoutData(gd);
        }


        UIUtils.setHelp(placeholder, IHelpContextIds.CTX_CON_WIZARD_DRIVER);
        UIUtils.asyncExec(() -> driverSelectViewer.getControl().setFocus());
    }

    public void createPanelDivider(Composite controlsGroup) {
        Composite filler = UIUtils.createComposite(controlsGroup, 3);
        filler.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        new Label(filler, SWT.NONE).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        new Label(filler, SWT.NONE).setImage(DBeaverIcons.getImage(UIIcon.SEPARATOR_V));
        new Label(filler, SWT.NONE).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }

    public DBPDriver getSelectedDriver()
    {
        return selectedDriver;
    }

    public void setSelectedDriver(DBPDriver selectedDriver) {
        this.selectedDriver = selectedDriver;
    }

    public DBPProject getConnectionProject() {
        return projectSelector.getSelectedProject();
    }

    public DBNBrowseSettings getNavigatorSettings() {
        return navigatorPreset.getSettings();
    }

    @Override
    public boolean canFlipToNextPage() {
        return this.projectSelector.getSelectedProject() != null && this.selectedDriver != null;
    }

    @Override
    public boolean isPageComplete()
    {
        return canFlipToNextPage();
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event)
    {
        this.selectedDriver = null;
        ISelection selection = event.getSelection();
        if (selection instanceof IStructuredSelection) {
            // TODO: Show current driver info somehow. setMessage is super-slow (it re-layouts entire wizard dialog)
            Object selectedObject = ((IStructuredSelection) selection).getFirstElement();
            if (selectedObject instanceof DBPDriver) {
                selectedDriver = (DriverDescriptor) selectedObject;
                //this.setMessage(selectedDriver.getDescription());
            } else if (selectedObject instanceof DataSourceProviderDescriptor) {
                //this.setMessage(((DataSourceProviderDescriptor) selectedObject).getDescription());
            } else if (selectedObject instanceof DriverTreeViewer.DriverCategory) {
                //this.setMessage(((DriverTreeViewer.DriverCategory) selectedObject).getName() + " drivers");
            } else {
                //this.setMessage("");
            }
        }
        getWizard().getContainer().updateButtons();
    }

    @Override
    public void doubleClick(DoubleClickEvent event)
    {
        if (selectedDriver != null) {
            wizard.getContainer().showPage(wizard.getNextPage(this));
        }
    }

    @Override
    public void activatePage()
    {
    }

    @Override
    public void deactivatePage()
    {

    }

}