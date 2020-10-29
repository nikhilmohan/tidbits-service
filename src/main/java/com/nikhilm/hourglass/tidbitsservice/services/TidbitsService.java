package com.nikhilm.hourglass.tidbitsservice.services;

import com.nikhilm.hourglass.tidbitsservice.repositories.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TidbitsService {

    @Autowired
    private TopicRepository topicsRepository;
}
