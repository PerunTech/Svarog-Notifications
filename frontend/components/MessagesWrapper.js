import { React, connect, PropTypes, GenericForm, elements, axios } from 'perun-core'
const { alertUser } = elements
import contextMenu from '../../backend/configuration/ContextMenu.json'
import style from './style/MessagesHolder.module.css'
import { iconManager } from './svgHolder';
import { flattenObject, jsonToURI } from '../utils/utils';
import InboxComponent from './InboxComponent';
import SentComponent from './SentComponent';
import ArchiveComponent from './ArchiveComponent';
import SearchComponent from './SearchComponent';

const tableName = 'MESSAGE'

class MessagesWrapper extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      contextMenuItems: contextMenu,
      inboxComponent: undefined,
      sentComponent: undefined,
      archiveComponent: undefined,
      searchComponent: undefined
    };

  }

  componentDidMount() {
    this.getContextMenu(this.state.contextMenuItems);
    this.setState({inboxComponent: <InboxComponent location={this.props.location} />})
  }

  getContextMenu = (contextMenuItems) => {
    let htmlElement
    let elementArr = []
    let className
    let contextMenu = contextMenuItems.navigation.items
    for (let i = 0; i < contextMenu.length; i++) {
      if (contextMenu[i].className) {
        className = 'create-new-msg'
      } else {
        className = 'context-menu-button'
      }
      htmlElement = <div className={style['context-menu-holder']}>
        <button onClick={() => this.doAction(contextMenu[i].id)} className={style[className]} key={contextMenu[i].id} id={contextMenu[i].id}>{iconManager.getIcon(contextMenu[i].icon)}{contextMenu[i].labelCode}</button>
      </div>
      elementArr.push(htmlElement)
    }
    this.setState({ generateElement: elementArr })
  }


  generateForm = () => {
    const { svSession } = this.props
    let customSaveButtonName = <div>{iconManager.getIcon('sent')}Send</div>
    let form = <GenericForm
      params={'READ_URL'}
      key={tableName + '_FORM'}
      id={tableName + '_FORM'}
      method={`/message_module/MsgServices/getTableJSONSchema/${svSession}/${tableName}`}
      uiSchemaConfigMethod={`/message_module/MsgServices/getTableUISchema/${svSession}/${tableName}`}
      hideBtns='closeAndDelete'
      addSaveFunction={(e) => this.saveForm(e)}
      customSave={true}
      customSaveButtonName={customSaveButtonName}
      className={'message-form'}
    />
    this.setState({ form: form, inboxComponent: undefined, sentComponent: undefined, archiveComponent: undefined})
  }


  saveForm = (e) => { 
    let { svSession } = this.props
    let postUrl = window.server + '/message_module/MsgServices/createNewMessage/' + svSession
    let form_params = e.formData
    // console.log(form_params)
    // console.log(flattenObject(form_params))
    const data = jsonToURI(flattenObject(form_params))
    // console.log(data)
    axios({
      method: 'post',
      data: data,
      url: postUrl,
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    }).then((response) => {
      if (response && response.data) {
        if (response.data) {
          alertUser(true, response.data.type.toLowerCase(), response.data.title, response.data.message, null)
          this.setState({form : ''}, () => {
            this.generateForm()
          })
        }
      }
    })
      .catch((error) => {
        if (error.data) {
          alertUser(true, error.data.type.toLowerCase(), response.data.title, error.data.message)
        }
      })
  }

  doAction(actionType) {
    if (actionType) {
      switch (actionType) {
        case 'search': {
          this.setState({ searchComponent: <SearchComponent />, sentComponent: undefined, form: undefined, archiveComponent: undefined, inboxComponent: undefined})
          break;
        }
        case 'create_message': {
          this.generateForm()
          this.setState({searchComponent: undefined, inboxComponent: undefined, archiveComponent: undefined, sentComponent: undefined })
          break;
        }
        case 'msg_inbox': {
          this.setState({ inboxComponent: <InboxComponent location={this.props.location} />, sentComponent: undefined, form: undefined, archiveComponent: undefined, searchComponent: undefined })
          break;
        }
        case 'msg_sent': {
          this.setState({ sentComponent: <SentComponent />, inboxComponent: undefined, form: undefined, archiveComponent: undefined, searchComponent: undefined })
          break;
        }
        case 'msg_archived': {
          this.setState({ archiveComponent: <ArchiveComponent />, inboxComponent: undefined, sentComponent: undefined, form: undefined, searchComponent: undefined })
          break;
        }
      }
    }
  }

  render() {
    const { generateElement, form, inboxComponent, sentComponent, archiveComponent,searchComponent } = this.state
    return (
      <div id='container' className={style['container']}>
        <div id='left-container' className={style['left-container']}>
          <p className={style['context-menu-paragraph']}>Folders</p>
          {generateElement}
          <div className={style['left-container-icon']}>
            {iconManager.getIcon('message-vector-icon')}
          </div>
        </div>
        <div id='right-container' className={style['right-container']}>
          {form}
          {inboxComponent}
          {sentComponent}
          {archiveComponent}
          {searchComponent}
        </div>
      </div>
    )
  }
}

const mapStateToProps = state => ({
  svSession: state.security.svSession
})

MessagesWrapper.contextTypes = {
  intl: PropTypes.object.isRequired
}

export default connect(mapStateToProps)(MessagesWrapper)