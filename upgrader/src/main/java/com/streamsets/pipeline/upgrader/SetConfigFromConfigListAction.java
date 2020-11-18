/*
 * Copyright 2020 StreamSets Inc.
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

package com.streamsets.pipeline.upgrader;

import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.api.StageUpgrader;
import com.streamsets.pipeline.api.impl.Utils;
import com.streamsets.pipeline.api.Config;
import java.util.List;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class SetConfigFromConfigListAction<T> extends UpgraderAction<SetConfigFromConfigListAction, T> {

  private String elementName;
  private String listName;
  private boolean deleteFieldInList;

  public SetConfigFromConfigListAction(Function<T, ConfigsAdapter> wrapper) {
    super(wrapper);
  }

  @Override
  public void upgrade(
      StageUpgrader.Context context, Map<String, Object> originalConfigs, T configs
  ) {
    Utils.checkNotNull(getName(), "name");
    Utils.checkNotNull(getListName(), "listName");
    Utils.checkNotNull(getElementName(), "elementName");
    Utils.checkNotNull(mustDeleteFieldInList(), "deleteFieldInList");

    ConfigsAdapter configsAdapter = wrap(configs);
    ConfigsAdapter.Pair configListPair = configsAdapter.find(getListName());

    if(configListPair == null) {
      throw new StageException(Errors.YAML_UPGRADER_03, getListName());
    }

    List<Config> configsInList = (List) configListPair.getValue();

    Optional pairWithConfigOptional =
        configsInList.stream().filter(pair -> pair.getName().equals(getElementName())).findFirst();

    if(!pairWithConfigOptional.isPresent()) {
      throw new StageException(Errors.YAML_UPGRADER_03, Utils.format("{} within list {}", getElementName(),
          getListName()));
    }

    Config pairWithConfig = (Config) pairWithConfigOptional.get();

    configsAdapter.set(getName(), pairWithConfig.getValue());

    if(mustDeleteFieldInList()) {
      configsInList.remove(pairWithConfig);
      configsAdapter.set(getListName(), configsInList);
    }

  }


  public String getElementName() {
    return elementName;
  }

  public SetConfigFromConfigListAction<T> setElementName(String elementName) {
    this.elementName = elementName;
    return this;
  }

  public boolean mustDeleteFieldInList() {
    return deleteFieldInList;
  }

  public SetConfigFromConfigListAction<T> setDeleteFieldInList(boolean deleteFieldInList) {
    this.deleteFieldInList = deleteFieldInList;
    return this;
  }


  public String getListName() {
    return listName;
  }

  public SetConfigFromConfigListAction<T> setListName(String listName) {
    this.listName = listName;
    return this;
  }
}
