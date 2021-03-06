package com.ecommerce.shopping.service;

import com.ecommerce.shopping.domain.Cart;
import com.ecommerce.shopping.domain.Product;
import com.ecommerce.shopping.domain.User;

import java.util.Optional;

/**
 * Created using Intellij IDE
 * Created by rnkoaa on 2/14/17.
 */
public interface CartService {

    Cart addItemToCart(User user, Product product);

    Cart addItemToCart(User user, Product product, int quantity);

    Cart addItemToCart(User user, Long productId);

    Cart addItemToCart(Long userId, Long productId);

    Cart removeItem(Long userId, Long productId);

    Cart removeItem(User user, Product product);

    Cart clear(User user);

    Optional<Cart> findByUser(User user);

    Optional<Cart> findByUsername(String username);

    Optional<Cart> findByUserId(Long id);

    Cart updateItem(User user, Product product, int itemCount);
}
