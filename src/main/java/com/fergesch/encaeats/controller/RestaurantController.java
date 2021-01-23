package com.fergesch.encaeats.controller;

import com.fergesch.encaeats.dao.RestaurantDao;
import com.fergesch.encaeats.dao.UserInteractionsDao;
import com.fergesch.encaeats.model.Restaurant;
import com.fergesch.encaeats.model.UserInteractions;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RequestMapping("/restaurant")
@Controller
public class RestaurantController {

    @Autowired
    private RestaurantDao restaurantDao;

    @Autowired
    private UserInteractionsDao userInteractionsDao;

    Gson gson = new Gson();

    private static final String USER_ID = "test_user_id";

    @GetMapping
    public ResponseEntity<String> restaurant(@RequestParam(name = "alias") String restaurantAlias) {
        Restaurant restaurant = restaurantDao.findRestaurantByAlias(restaurantAlias);
        if(restaurant != null) {
            Map<String, String> params = new HashMap<>();
            params.put("rest_alias", restaurantAlias);
            params.put("user_id", USER_ID);
            UserInteractions userInteraction = userInteractionsDao.findUserInteractionsByAlias(params);
            userInteraction.setUser_id(USER_ID);
            userInteraction.setRest_alias(restaurantAlias);
            restaurant.setUserInteractions(userInteraction);

            return new ResponseEntity<>(gson.toJson(restaurant), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/search")
    public ResponseEntity<String> restaurantSearch(
            @RequestParam Map<String, String> searchCriteria) {
        if(!validateSearchCriteria(searchCriteria)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Set<Restaurant> restaurantResults = restaurantDao.restaurantSearch(searchCriteria);
        if(restaurantResults.size() > 0) {
            List<String> restaurantAliases =
                    restaurantResults.stream().map(Restaurant::getAlias)
                    .collect(Collectors.toList());
            HashMap<String, UserInteractions> userInteractions = userInteractionsDao.multiGetUserInteractions(USER_ID, restaurantAliases);
            Set<Restaurant> result = restaurantResults.stream()
                    .map(restaurant -> {
                        UserInteractions userInteraction = userInteractions.getOrDefault(restaurant.getAlias(),
                                new UserInteractions(USER_ID, restaurant.getAlias()));
                        restaurant.setUserInteractions(userInteraction);
                        return filterUserInteractions(restaurant, searchCriteria);})
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            return result.size() > 0 ?
                    new ResponseEntity<>(gson.toJson(result), HttpStatus.OK):
                    new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    private boolean validateSearchCriteria(Map<String, String> searchCriteria) {
        for(String key : restaurantDao.getSearchParams())
        {
            //@TODO maybe add some kind of validation on the string
            if(searchCriteria.get(key) != null) {
                return true;
            }
        }
        return false;
    }

    private Restaurant filterUserInteractions(Restaurant r, Map<String, String> searchCriteria) {

        //filter based on wish_list
        if(searchCriteria.get(RestaurantDao.WISH_LIST) != null) {
            Boolean value = Boolean.parseBoolean(searchCriteria.get(RestaurantDao.WISH_LIST));
            r = value.equals(r.getUserInteractions().getWish_list().getBool()) ? r : null;
        }

        //filter based on visited
        if(r != null && searchCriteria.get(RestaurantDao.VISITED) != null) {
            Boolean value = Boolean.parseBoolean(searchCriteria.get(RestaurantDao.VISITED));
            r = value.equals(r.getUserInteractions().getVisited().getBool()) ? r : null;
        }
        return r;
    }
}


