/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config.pluggabletask;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.config.AntTask;
import com.thoughtworks.go.domain.TaskProperty;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.DataStructureUtils;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PluggableTaskTest {
    @Test
    public void testConfigAsMap() throws Exception {
        PluginConfiguration pluginConfiguration = new PluginConfiguration("test-plugin-id", "13.4");

        GoCipher cipher = new GoCipher();
        List<String> keys = Arrays.asList("Avengers 1", "Avengers 2", "Avengers 3", "Avengers 4");
        List<String> values = Arrays.asList("Iron man", "Hulk", "Thor", "Captain America");

        Configuration configuration = new Configuration(
                new ConfigurationProperty(new ConfigurationKey(keys.get(0)), new ConfigurationValue(values.get(0))),
                new ConfigurationProperty(new ConfigurationKey(keys.get(1)), new ConfigurationValue(values.get(1))),
                new ConfigurationProperty(new ConfigurationKey(keys.get(2)), new ConfigurationValue(values.get(2))),
                new ConfigurationProperty(new ConfigurationKey(keys.get(3)), new ConfigurationValue(values.get(3)),
                        new EncryptedConfigurationValue(cipher.encrypt(values.get(3))), cipher));

        PluggableTask task = new PluggableTask("test-task", pluginConfiguration, configuration);

        Map<String, Map<String, String>> configMap = task.configAsMap();
        assertThat(configMap.keySet().size(), is(keys.size()));
        assertThat(configMap.values().size(), is(values.size()));
        assertThat(configMap.keySet().containsAll(keys), is(true));
        for (int i = 0; i < keys.size(); i++) {
            assertThat(configMap.get(keys.get(i)).get(PluggableTask.VALUE_KEY), is(values.get(i)));
        }
    }

    @Test
    public void shouldReturnTrueWhenPluginConfigurationForTwoPluggableTasksIsExactlyTheSame() {
        PluginConfiguration pluginConfiguration = new PluginConfiguration("test-plugin-1", "1.0");
        PluggableTask pluggableTask1 = new PluggableTask("test-task-1", pluginConfiguration, new Configuration());
        PluggableTask pluggableTask2 = new PluggableTask("test-task-2", pluginConfiguration, new Configuration());
        assertTrue(pluggableTask1.hasSameTypeAs(pluggableTask2));
    }

    @Test
    public void shouldReturnFalseWhenPluginConfigurationForTwoPluggableTasksIsDifferent() {
        PluginConfiguration pluginConfiguration1 = new PluginConfiguration("test-plugin-1", "1.0");
        PluginConfiguration pluginConfiguration2 = new PluginConfiguration("test-plugin-2", "1.0");
        PluggableTask pluggableTask1 = new PluggableTask("test-task-1", pluginConfiguration1, new Configuration());
        PluggableTask pluggableTask2 = new PluggableTask("test-task-2", pluginConfiguration2, new Configuration());
        assertFalse(pluggableTask1.hasSameTypeAs(pluggableTask2));
    }

    @Test
    public void shouldReturnFalseWhenPluggableTaskIsComparedWithAnyOtherTask() {
        PluginConfiguration pluginConfiguration = new PluginConfiguration("test-plugin-1", "1.0");
        PluggableTask pluggableTask = new PluggableTask("test-task-1", pluginConfiguration, new Configuration());
        AntTask antTask = new AntTask();
        assertFalse(pluggableTask.hasSameTypeAs(antTask));
    }

    @Test
    public void taskTypeShouldBeSanitizedToHaveNoSpecialCharacters() throws Exception {
        assertThat(new PluggableTask("", new PluginConfiguration("abc.def", "1"), new Configuration()).getTaskType(), is("pluggable_task_abc_def"));
        assertThat(new PluggableTask("", new PluginConfiguration("abc_def", "1"), new Configuration()).getTaskType(), is("pluggable_task_abc_def"));
        assertThat(new PluggableTask("", new PluginConfiguration("abcdef", "1"), new Configuration()).getTaskType(), is("pluggable_task_abcdef"));
        assertThat(new PluggableTask("", new PluginConfiguration("abc#def", "1"), new Configuration()).getTaskType(), is("pluggable_task_abc_def"));
        assertThat(new PluggableTask("", new PluginConfiguration("abc#__def", "1"), new Configuration()).getTaskType(), is("pluggable_task_abc___def"));
        assertThat(new PluggableTask("", new PluginConfiguration("Abc#dEF", "1"), new Configuration()).getTaskType(), is("pluggable_task_Abc_dEF"));
        assertThat(new PluggableTask("", new PluginConfiguration("1234567890#ABCDEF", "1"), new Configuration()).getTaskType(), is("pluggable_task_1234567890_ABCDEF"));
    }

    @Test
    public void shouldPopulatePropertiesForDisplay() throws Exception {
        Configuration configuration = new Configuration(
                ConfigurationPropertyMother.create("KEY1", false, "value1"),
                ConfigurationPropertyMother.create("Key2", false, "value2"),
                ConfigurationPropertyMother.create("key3", true, "encryptedValue1"));

        PluggableTask task = new PluggableTask("abc", new PluginConfiguration("abc.def", "1"), configuration);

        List<TaskProperty> propertiesForDisplay = task.getPropertiesForDisplay();

        assertThat(propertiesForDisplay.size(), is(3));
        assertProperty(propertiesForDisplay.get(0), "KEY1", "value1", "key1");
        assertProperty(propertiesForDisplay.get(1), "Key2", "value2", "key2");
        assertProperty(propertiesForDisplay.get(2), "key3", "****", "key3");
    }

    @Test
    public void shouldPopulateItselfFromConfigAttributesMap() throws Exception {
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"), ConfigurationPropertyMother.create("Key2"));

        PluggableTask task = new PluggableTask("abc", new PluginConfiguration("abc.def", "1"), configuration);
        Map<String, String> attributeMap = DataStructureUtils.m("KEY1", "value1", "Key2", "value2");

        task.setTaskConfigAttributes(attributeMap);

        assertThat(task.configAsMap().get("KEY1").get(PluggableTask.VALUE_KEY), is("value1"));
        assertThat(task.configAsMap().get("Key2").get(PluggableTask.VALUE_KEY), is("value2"));
    }

    @Test
    public void shouldNotOverwriteValuesIfTheyAreNotAvailableInConfigAttributesMap() throws Exception {
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"), ConfigurationPropertyMother.create("Key2"));

        PluggableTask task = new PluggableTask("abc", new PluginConfiguration("abc.def", "1"), configuration);
        Map<String, String> attributeMap = DataStructureUtils.m("KEY1", "value1");

        task.setTaskConfigAttributes(attributeMap);

        assertThat(task.configAsMap().get("KEY1").get(PluggableTask.VALUE_KEY), is("value1"));
        assertThat(task.configAsMap().get("Key2").get(PluggableTask.VALUE_KEY), is(nullValue()));
    }

    @Test
    public void shouldIgnoreKeysPresentInConfigAttributesMapButNotPresentInConfiguration() throws Exception {
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("KEY1"));

        PluggableTask task = new PluggableTask("abc", new PluginConfiguration("abc.def", "1"), configuration);
        Map<String, String> attributeMap = DataStructureUtils.m("KEY1", "value1", "Key2", "value2");

        task.setTaskConfigAttributes(attributeMap);

        assertThat(task.configAsMap().get("KEY1").get(PluggableTask.VALUE_KEY), is("value1"));
        assertFalse(task.configAsMap().containsKey("Key2"));
    }

    private void assertProperty(TaskProperty taskProperty, String name, String value, String cssClass) {
        assertThat(taskProperty.getName(), is(name));
        assertThat(taskProperty.getValue(), is(value));
        assertThat(taskProperty.getCssClass(), is(cssClass));
    }
}