package com.faforever.moderatorclient.mapstruct;

import com.faforever.moderatorclient.api.dto.GamePlayerStats;
import com.faforever.moderatorclient.ui.domain.GamePlayerStatsFX;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(uses = {JavaFXMapper.class, GameMapper.class, PlayerMapper.class, CycleAvoidingMappingContext.class})
public abstract class GamePlayerStatsMapper {

    @Mapping(target = "beforeRating", ignore = true)
    @Mapping(target = "afterRating", ignore = true)
    @Mapping(target = "ratingChange", ignore = true)
    public abstract GamePlayerStatsFX map(GamePlayerStats gamePlayerStats);

    public abstract GamePlayerStats map(GamePlayerStatsFX gamePlayerStatsFX);

    public abstract List<GamePlayerStatsFX> map(List<GamePlayerStats> gamePlayerStats);
}