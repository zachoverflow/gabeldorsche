# Gabeldorsche
*A notification-focused accessory project to complement your Android phone and other wearables*

## The problem
* Everyone gets notifications
* Phones promised to bring us all closer together
  * Globally that's true; but locally they've tended to have the opposite effect
* Watches promised to help save us from the tyranny of our phones
  * But nope, looking at your watch while hanging out with people probably sends worse social signals than looking at your phone

## The delimma
You're basically faced with two options:

1. Prioritize the people you're hanging out with (and maybe miss an actually important notification) 
2. Prioritize your notifications (at the loss of focus on the people you're with)

Neither option is necessarily ideal. This what gabeldorsche tries to address.

## Towards a solution

### Improve notification ignorability, using vibration
* Encode the app in a vibration pattern
  * Some apps are more ignorable than others, depending on the context
* Encode the sender in a secondary vibration pattern (if parsable from the notification)
  * Some people are more ignorable than others, depending on the context

### Add another dimension to vibration
* Most devices really only have one axis to play with: time
* Gabeldorsche adds another axis: space
  * Currently 4 motors placed around a belt: front-left, front-right, back-right, and back-left
  * This gives us a broader encoding space - improving mnemonic qualities

### Allow the sender to help indicate urgency
* If something is worthy of pulling you away from the people you might be hanging out with, the sender can include #urgent in their message and gabeldorsche will indicate it to you
* [TODO: sketch out abuse handling, probably boy-who-cried-wolf style]
