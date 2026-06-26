package org.hoyo.celestia.user.repository;

import org.hoyo.celestia.user.DTOs.NoRefreshUserDTO;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryImpl implements UserRepositoryCustom {


    private final MongoTemplate mongoTemplate;

    public UserRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public NoRefreshUserDTO findUserCardByUid(String uid) {

        MatchOperation match = Aggregation.match(Criteria.where("uid").is(uid));
        ProjectionOperation projection = Aggregation.project()
                .and("detailInfo.nickname").as("nickname")
                .and("detailInfo.signature").as("signature")
                .and("detailInfo.headIcon").as("headIcon")
                .and("detailInfo.level").as("level")
                .and("detailInfo.recordInfo.achievementCount").as("achievementCount")
                .and("detailInfo.isDisplayAvatar").as("buildsPublic");


        Aggregation aggregation = Aggregation.newAggregation(match, projection, Aggregation.limit(1));
        AggregationResults<NoRefreshUserDTO> results = mongoTemplate.aggregate(aggregation, "user", NoRefreshUserDTO.class);

        NoRefreshUserDTO noRefreshUserDTO = results.getUniqueMappedResult();
        if (noRefreshUserDTO != null) {
            noRefreshUserDTO.setUid(uid);
        }

        return noRefreshUserDTO;
    }
}
