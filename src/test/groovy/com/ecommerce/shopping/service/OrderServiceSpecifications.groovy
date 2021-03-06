package com.ecommerce.shopping.service

import com.ecommerce.shopping.ShoppingCartApplication
import com.ecommerce.shopping.domain.*
import com.ecommerce.shopping.util.CartEmptyException
import com.ecommerce.shopping.util.InvalidOrderOperation
import com.ecommerce.shopping.util.UserNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

import static org.assertj.core.api.Java6Assertions.assertThat

/**
 * Created on 2/16/2017.
 */
@ContextConfiguration(classes = ShoppingCartApplication.class)
@Transactional
class OrderServiceSpecifications extends Specification {
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceSpecifications.class)
    @Autowired
    private OrderService orderService
    
    @Autowired
    private UserService userService
    
    @Autowired
    private ProductService productService
    
    @Autowired
    private CartService cartService
    
    def "context loads successfully"() {
        expect:
            assertThat(orderService).isNotNull();
    }
    
    def "a non registered user cannot create an order"() {
        given:
            def user = User.builder().firstName("Richard").lastName("Amoako").username("richard").build()
        
        when:
            orderService.createOrder(user)
        
        then:
            thrown(UserNotFoundException.class)
    }
    
    def "a registered user cannot create an order if their cart is empty"() {
        given:
            def user = User.builder().firstName("Richard").lastName("Amoako").username("richard").build()
        
        when:
            user = userService.save(user)
        
        then:
            user != null
            user.id > 0L
        
        when:
            orderService.createOrder(user)
        
        then:
            thrown(CartEmptyException.class)
    }
    
    def "A user can create an order by adding items to the cart first"() {
        given:
            def user = User.builder().firstName("Richard").lastName("Amoako").username("richard").build()
            def product = Product.builder().name("Product-1").serialNumber("1234").build()
            def cart = Cart.builder().build()
            
            cart.setUser(user)
            user.setCart(cart)
        
        when:
            user = userService.save(user)
            product = productService.save(product)
        
        
        then:
            user != null
            user.id > 0L
            user.cart != null
        
        and:
            product != null
            product.id > 0L
        
        when:
            cart = cartService.addItemToCart(user, product)
        
        then:
            cart != null
            cart.items.size() > 0 && cart.items.size() == 1
        
        when:
            def order = orderService.createOrder(user)
        
        then:
            notThrown(CartEmptyException.class)
            order != null
            assertThat(order.getId()).isNotNull().isGreaterThan(0L)
            assertThat(order.getOrderKey()).isNotNull().isNotEmpty();
            assertThat(order.isEmpty()).isFalse()
            assertThat(order.getItems()).isNotNull().hasSize(1)
    }
    
    def "After an order is created, the user can add a billing address"() {
        given:
            def user = User.builder().firstName("Richard").lastName("Amoako").username("richard").build()
            def product = Product.builder().name("Product-1").serialNumber("1234").build()
            def cart = Cart.builder().build()
            
            cart.setUser(user)
            user.setCart(cart)
            
            user = userService.save(user)
            product = productService.save(product)
            cart = cartService.addItemToCart(user, product)
            def order = orderService.createOrder(user)
            
            def billingAddress = Address.builder().user(user).street("2698 lower 147th Ct. W.").city("Rosemount").state("MN").zipCode("55068").build();
            
            user.addAddress(billingAddress)
            
            user = userService.save(user)
        when:
            
            def addresses = user.getAddresses()
            def addressList = new ArrayList<>(addresses)
            def savedAddress = addressList.get(0)
        
        then:
            assertThat(savedAddress).isNotNull()
            assertThat(savedAddress.getId()).isNotNull();
        
        when:
            order.setBillingAddress(billingAddress)
            def order1 = orderService.save(order)
        
        then:
            assertThat(order1.getBillingAddress()).isNotNull()
            assertThat(order1.getId()).isEqualTo(order.getId())
    }
    
    def "After an order is created, the user can add a shipping address"() {
        given:
            def user = User.builder().firstName("Richard").lastName("Amoako").username("richard").build()
            def product = Product.builder().name("Product-1").serialNumber("1234").build()
            def cart = Cart.builder().build()
            
            cart.setUser(user)
            user.setCart(cart)
            
            user = userService.save(user)
            product = productService.save(product)
            cart = cartService.addItemToCart(user, product)
            def order = orderService.createOrder(user)
            
            def billingAddress = Address.builder().user(user).street("2698 lower 147th Ct. W.").city("Rosemount").state("MN").zipCode("55068").build();
            
            user.addAddress(billingAddress)
            
            user = userService.save(user)
            
            def addresses = user.getAddresses()
            def addressList = new ArrayList<>(addresses)
            def savedAddress = addressList.get(0)
            order.setBillingAddress(billingAddress)
            def order1 = orderService.save(order)
        
        when:
            
            order.setShippingAddress(billingAddress)
            def order2 = orderService.save(order)
        then:
            assertThat(order2.getShippingAddress()).isNotNull()
            assertThat(order2.getShippingAddress()).isEqualTo(order2.getBillingAddress())
            assertThat(order2.getId()).isEqualTo(order.getId())
    }
    
    def "Apply A payment to a completed order"() {
        given:
            def user = User.builder().firstName("Richard").lastName("Amoako").username("richard").build()
            PaymentMethod paymentMethod = PaymentMethod.builder().name("Richard Agyei").paymentType(PaymentType.AMERICAN_EXPRESS).user(user).build()
            def product = Product.builder().name("Product-1").serialNumber("1234").build()
            def cart = Cart.builder().build()
            
            cart.setUser(user)
            user.setCart(cart)
            
            user = userService.save(user)
            user.addPaymentMethod(paymentMethod)
            
            product = productService.save(product)
            cart = cartService.addItemToCart(user, product)
            def order = orderService.createOrder(user)
            
            def billingAddress = Address.builder().user(user).street("2698 lower 147th Ct. W.").city("Rosemount").state("MN").zipCode("55068").build();
            
            user.addAddress(billingAddress)
            
            user = userService.save(user)
            
            def addresses = user.getAddresses()
            def addressList = new ArrayList<>(addresses)
            def savedAddress = addressList.get(0)
            order.setBillingAddress(billingAddress)
            order = orderService.save(order)
            order.setShippingAddress(billingAddress)
            order = orderService.save(order)
            
            List<PaymentMethod> payments = new ArrayList<>(user.getPaymentMethods())
            
            paymentMethod = payments.get(0)
        
        when:
            order = orderService.applyPayment(order, paymentMethod)
        
        then:
            assertThat(order.payments).hasSize(1)
            assertThat(order.orderStatus).isEqualTo(OrderStatus.PAID)
    }
    
    def "An order which is not yet completed can be cancelled"() {
        given:
            def user = User.builder().firstName("Richard").lastName("Amoako").username("richard").build()
            PaymentMethod paymentMethod = PaymentMethod.builder().name("Richard Agyei").paymentType(PaymentType.AMERICAN_EXPRESS).user(user).build()
            def product = Product.builder().name("Product-1").serialNumber("1234").build()
            def cart = Cart.builder().build()
            
            cart.setUser(user)
            user.setCart(cart)
            
            user = userService.save(user)
            user.addPaymentMethod(paymentMethod)
            
            product = productService.save(product)
            cart = cartService.addItemToCart(user, product)
            def order = orderService.createOrder(user)
            
            def billingAddress = Address.builder().user(user).street("2698 lower 147th Ct. W.").city("Rosemount").state("MN").zipCode("55068").build();
            
            user.addAddress(billingAddress)
            
            user = userService.save(user)
            
            def addresses = user.getAddresses()
            def addressList = new ArrayList<>(addresses)
            def savedAddress = addressList.get(0)
            order.setBillingAddress(billingAddress)
            order = orderService.save(order)
            order.setShippingAddress(billingAddress)
            order = orderService.save(order)
            
            List<PaymentMethod> payments = new ArrayList<>(user.getPaymentMethods())
            
            paymentMethod = payments.get(0)
            order = orderService.applyPayment(order, paymentMethod)
        
        when:
            order = orderService.cancel(order);
        then:
            // assertThat(order.payments).hasSize(1)
            assertThat(order.orderStatus).isEqualTo(OrderStatus.CANCELLED)
    }
    
    def "A payment can be applied to an order that is no cancelled"() {
        given:
            def user = User.builder().firstName("Richard").lastName("Amoako").username("richard").build()
            PaymentMethod paymentMethod = PaymentMethod.builder().name("Richard Agyei").paymentType(PaymentType.AMERICAN_EXPRESS).user(user).build()
            def product = Product.builder().name("Product-1").serialNumber("1234").build()
            def cart = Cart.builder().build()
            
            cart.setUser(user)
            user.setCart(cart)
            
            user = userService.save(user)
            user.addPaymentMethod(paymentMethod)
            
            product = productService.save(product)
            cart = cartService.addItemToCart(user, product)
            def order = orderService.createOrder(user)
            
            def billingAddress = Address.builder().user(user).street("2698 lower 147th Ct. W.").city("Rosemount").state("MN").zipCode("55068").build();
            
            user.addAddress(billingAddress)
            
            user = userService.save(user)
            
            def addresses = user.getAddresses()
            def addressList = new ArrayList<>(addresses)
            def savedAddress = addressList.get(0)
            order.setBillingAddress(billingAddress)
            order = orderService.save(order)
            order.setShippingAddress(billingAddress)
            order = orderService.save(order)
            
            List<PaymentMethod> payments = new ArrayList<>(user.getPaymentMethods())
            
            paymentMethod = payments.get(0)
            order = orderService.applyPayment(order, paymentMethod)
        
        expect:
            order.getOrderStatus() == OrderStatus.PAID
        
        when:
            order = orderService.cancel(order);
        then:
            logger.info("Order Status: {}", order.orderStatus)
            notThrown(InvalidOrderOperation.class)
            order.orderStatus == OrderStatus.CANCELLED
    }
    
    def "A completed Order cannot be cancelled"() {
        given:
            def user = User.builder().firstName("Richard").lastName("Amoako").username("richard").build()
            PaymentMethod paymentMethod = PaymentMethod.builder().name("Richard Agyei").paymentType(PaymentType.AMERICAN_EXPRESS).user(user).build()
            def product = Product.builder().name("Product-1").serialNumber("1234").build()
            def cart = Cart.builder().build()
            
            cart.setUser(user)
            user.setCart(cart)
            
            user = userService.save(user)
            user.addPaymentMethod(paymentMethod)
            
            product = productService.save(product)
            cart = cartService.addItemToCart(user, product)
            def order = orderService.createOrder(user)
            
            def billingAddress = Address.builder().user(user).street("2698 lower 147th Ct. W.").city("Rosemount").state("MN").zipCode("55068").build();
            
            user.addAddress(billingAddress)
            
            user = userService.save(user)
            
            def addresses = user.getAddresses()
            def addressList = new ArrayList<>(addresses)
            def savedAddress = addressList.get(0)
            order.setBillingAddress(billingAddress)
            order = orderService.save(order)
            order.setShippingAddress(billingAddress)
            order.setOrderStatus(OrderStatus.COMPLETED)
            order = orderService.save(order)
        
        expect:
            order.getOrderStatus() == OrderStatus.COMPLETED
        
        when:
            order = orderService.cancel(order);
        then:
            thrown(InvalidOrderOperation.class)
    }
    
    def "A Shipped Order cannot be cancelled"() {
        given:
            def user = User.builder().firstName("Richard").lastName("Amoako").username("richard").build()
            PaymentMethod paymentMethod = PaymentMethod.builder().name("Richard Agyei").paymentType(PaymentType.AMERICAN_EXPRESS).user(user).build()
            def product = Product.builder().name("Product-1").serialNumber("1234").build()
            def cart = Cart.builder().build()
            
            cart.setUser(user)
            user.setCart(cart)
            
            user = userService.save(user)
            user.addPaymentMethod(paymentMethod)
            
            product = productService.save(product)
            cart = cartService.addItemToCart(user, product)
            def order = orderService.createOrder(user)
            
            def billingAddress = Address.builder().user(user).street("2698 lower 147th Ct. W.").city("Rosemount").state("MN").zipCode("55068").build();
            
            user.addAddress(billingAddress)
            
            user = userService.save(user)
            
            def addresses = user.getAddresses()
            def addressList = new ArrayList<>(addresses)
            def savedAddress = addressList.get(0)
            order.setBillingAddress(billingAddress)
            order = orderService.save(order)
            order.setShippingAddress(billingAddress)
            order.setOrderStatus(OrderStatus.SHIPPED)
            order = orderService.save(order)
        
        expect:
            order.getOrderStatus() == OrderStatus.SHIPPED
        
        when:
            order = orderService.cancel(order);
        then:
            thrown(InvalidOrderOperation.class)
    }
    
    def "Cannot apply payment to a cancelled order"() {
        given:
            def user = User.builder().firstName("Richard").lastName("Amoako").username("richard").build()
            PaymentMethod paymentMethod = PaymentMethod.builder().name("Richard Agyei").paymentType(PaymentType.AMERICAN_EXPRESS).user(user).build()
            def product = Product.builder().name("Product-1").serialNumber("1234").build()
            def cart = Cart.builder().build()
            
            cart.setUser(user)
            user.setCart(cart)
            
            user = userService.save(user)
            user.addPaymentMethod(paymentMethod)
            
            product = productService.save(product)
            cart = cartService.addItemToCart(user, product)
            def order = orderService.createOrder(user)
            
            def billingAddress = Address.builder().user(user).street("2698 lower 147th Ct. W.").city("Rosemount").state("MN").zipCode("55068").build();
            
            user.addAddress(billingAddress)
            
            user = userService.save(user)
            
            def addresses = user.getAddresses()
            def addressList = new ArrayList<>(addresses)
            def savedAddress = addressList.get(0)
            order.setBillingAddress(billingAddress)
            order = orderService.save(order)
            order.setShippingAddress(billingAddress)
            order.setOrderStatus(OrderStatus.CANCELLED)
            order = orderService.save(order)
        
        expect:
            order.orderStatus == OrderStatus.CANCELLED
        
        when:
            List<PaymentMethod> payments = new ArrayList<>(user.getPaymentMethods())
            paymentMethod = payments.get(0)
            order = orderService.applyPayment(order, paymentMethod)
        then:
            thrown(InvalidOrderOperation.class)
    }
    
    def "Cannot apply payment to a shipped order"() {
        given:
            def user = User.builder().firstName("Richard").lastName("Amoako").username("richard").build()
            PaymentMethod paymentMethod = PaymentMethod.builder().name("Richard Agyei").paymentType(PaymentType.AMERICAN_EXPRESS).user(user).build()
            def product = Product.builder().name("Product-1").serialNumber("1234").build()
            def cart = Cart.builder().build()
            
            cart.setUser(user)
            user.setCart(cart)
            
            user = userService.save(user)
            user.addPaymentMethod(paymentMethod)
            
            product = productService.save(product)
            cart = cartService.addItemToCart(user, product)
            def order = orderService.createOrder(user)
            
            def billingAddress = Address.builder().user(user).street("2698 lower 147th Ct. W.").city("Rosemount").state("MN").zipCode("55068").build();
            
            user.addAddress(billingAddress)
            
            user = userService.save(user)
            
            def addresses = user.getAddresses()
            def addressList = new ArrayList<>(addresses)
            def savedAddress = addressList.get(0)
            order.setBillingAddress(billingAddress)
            order = orderService.save(order)
            order.setShippingAddress(billingAddress)
            order.setOrderStatus(OrderStatus.SHIPPED)
            order = orderService.save(order)
        
        expect:
            order.orderStatus == OrderStatus.SHIPPED
        
        when:
            List<PaymentMethod> payments = new ArrayList<>(user.getPaymentMethods())
            paymentMethod = payments.get(0)
            order = orderService.applyPayment(order, paymentMethod)
        then:
            thrown(InvalidOrderOperation.class)
    }
}