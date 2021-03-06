package com.faforever.moderatorclient.mapstruct;

import com.faforever.moderatorclient.api.dto.FeaturedMod;
import com.faforever.moderatorclient.ui.domain.FeaturedModFX;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(uses = JavaFXMapper.class)
public interface FeaturedModMapper {
    FeaturedModFX map(FeaturedMod featuredMod);

    FeaturedMod map(FeaturedModFX featuredModFX);

    List<FeaturedModFX> map(List<FeaturedMod> featuredMods);
}